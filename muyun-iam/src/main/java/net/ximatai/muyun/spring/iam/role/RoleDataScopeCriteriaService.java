package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RoleDataScopeCriteriaService implements DataScopeCriteriaService {
    private final RoleService roleService;
    private final Optional<OrganizationService> organizationService;

    public RoleDataScopeCriteriaService(RoleService roleService) {
        this(roleService, Optional.empty());
    }

    @Autowired
    public RoleDataScopeCriteriaService(RoleService roleService, Optional<OrganizationService> organizationService) {
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
        this.organizationService = organizationService == null ? Optional.empty() : organizationService;
    }

    @Override
    public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                    String actionCode,
                                                    Criteria criteria,
                                                    Optional<CurrentUser> currentUser) {
        return resolveReadScope(moduleAlias, policyOf(actionCode), criteria, currentUser);
    }

    @Override
    public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                    ActionExecutionPolicy policy,
                                                    Criteria criteria,
                                                    Optional<CurrentUser> currentUser) {
        Objects.requireNonNull(policy, "policy must not be null");
        Criteria base = criteria == null ? Criteria.of() : criteria;
        CurrentUser user = currentUser.orElse(null);
        if (user == null) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        if (user.system()) {
            return DataScopeCriteriaResult.unrestricted(base);
        }
        List<RoleAction> grants = roleService.effectiveActionGrants(
                user.userId(), moduleAlias, policy.permissionActionCode());
        List<GrantScope> scopes = grantScopes(policy, user, grants);
        if (scopes.isEmpty()) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        return combineGrantedScopes(base, user, scopes, grants.stream().anyMatch(this::allowsCrossTenant));
    }

    private ActionExecutionPolicy policyOf(String actionCode) {
        return new ActionExecutionPolicy(
                actionCode,
                null,
                null,
                true,
                true,
                ActionDefaultGrantPolicy.NONE,
                null
        );
    }

    private List<GrantScope> grantScopes(ActionExecutionPolicy policy, CurrentUser user, List<RoleAction> grants) {
        java.util.ArrayList<GrantScope> scopes = new java.util.ArrayList<>();
        GrantScope defaultScope = resolveDefaultScope(policy.defaultGrantPolicy(), user);
        if (defaultScope.contributes()) {
            scopes.add(defaultScope);
        }
        if (grants != null) {
            grants.stream()
                    .map(grant -> resolveGrantScope(grant, user))
                    .filter(GrantScope::contributes)
                    .forEach(scopes::add);
        }
        return List.copyOf(scopes);
    }

    private DataScopeCriteriaResult combineGrantedScopes(Criteria base,
                                                         CurrentUser user,
                                                         List<GrantScope> scopes,
                                                         boolean hasCrossTenantGrant) {
        Criteria combinedScope = Criteria.of();
        boolean contributedCrossTenantScope = false;

        for (GrantScope grantScope : scopes) {
            if (grantScope.allData()) {
                DataScopeCriteriaResult result = resolveAllScope(base, grantScope.crossTenant(), hasCrossTenantGrant);
                if (result != null) {
                    return result;
                }
                appendCurrentTenantScope(combinedScope, user);
                continue;
            }
            appendGrantScope(combinedScope, grantScope, user, hasCrossTenantGrant);
            contributedCrossTenantScope = contributedCrossTenantScope || grantScope.crossTenant();
        }

        if (combinedScope.isEmpty()) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }

        Criteria scoped = combine(base, combinedScope);
        return contributedCrossTenantScope
                ? DataScopeCriteriaResult.crossTenantRestricted(scoped)
                : DataScopeCriteriaResult.restricted(scoped);
    }

    @Override
    public Criteria applyReadScope(String moduleAlias,
                                   String actionCode,
                                   Criteria criteria,
                                   Optional<CurrentUser> currentUser) {
        return resolveReadScope(moduleAlias, actionCode, criteria, currentUser).criteria();
    }

    private DataScopeCriteriaResult resolveAllScope(Criteria base,
                                                    boolean grantCrossTenant,
                                                    boolean hasCrossTenantGrant) {
        if (grantCrossTenant) {
            return DataScopeCriteriaResult.crossTenantUnrestricted(base);
        }
        if (!hasCrossTenantGrant) {
            return DataScopeCriteriaResult.unrestricted(base);
        }
        return null;
    }

    private void appendGrantScope(Criteria combinedScope,
                                  GrantScope grantScope,
                                  CurrentUser user,
                                  boolean hasCrossTenantGrant) {
        if (hasCrossTenantGrant && !grantScope.crossTenant()) {
            combinedScope.orGroup(currentTenantScoped(grantScope.criteria(), user).getRoot());
            return;
        }
        combinedScope.orGroup(grantScope.criteria().getRoot());
    }

    private GrantScope resolveGrantScope(RoleAction grant, CurrentUser user) {
        DataScopePolicy policy = normalizePolicy(grant);
        if (policy == DataScopePolicy.ALL) {
            return GrantScope.all(allowsCrossTenant(grant));
        }
        Criteria criteria = criteriaForPolicy(policy, user);
        return GrantScope.restricted(criteria, allowsCrossTenant(grant));
    }

    private GrantScope resolveDefaultScope(ActionDefaultGrantPolicy policy, CurrentUser user) {
        return switch (normalizeDefaultPolicy(policy)) {
            case NONE, ANY_LOGIN_USER -> GrantScope.none();
            case OWNER -> GrantScope.restricted(criteriaForPolicies(user, DataScopePolicy.OWNER), false);
            case ASSIGNEE -> GrantScope.restricted(criteriaForPolicies(
                    user, DataScopePolicy.OWNER, DataScopePolicy.ASSIGNEE), false);
            case MEMBER -> GrantScope.restricted(criteriaForPolicies(
                    user, DataScopePolicy.OWNER, DataScopePolicy.ASSIGNEE, DataScopePolicy.MEMBER), false);
        };
    }

    private Criteria criteriaForPolicy(DataScopePolicy policy, CurrentUser user) {
        return criteriaForPolicies(user, policy);
    }

    private Criteria criteriaForPolicies(CurrentUser user, DataScopePolicy... policies) {
        Criteria scope = Criteria.of();
        if (policies != null) {
            for (DataScopePolicy policy : policies) {
                appendScope(scope, policy, user);
            }
        }
        return scope;
    }

    private void appendScope(Criteria scope, DataScopePolicy policy, CurrentUser user) {
        switch (normalizePolicy(policy)) {
            case OWNER -> scope.orEq(PlatformAbilityFields.AUTH_USER_FIELD, user.userId());
            case ASSIGNEE -> scope.orRaw(csvContains(PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN, "userId", user.userId()));
            case MEMBER -> scope.orRaw(csvContains(PlatformAbilityFields.AUTH_MEMBER_COLUMN, "userId", user.userId()));
            case ORGANIZATION -> {
                if (user.organizationId() != null) {
                    scope.orEq(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, user.organizationId());
                }
            }
            case ORGANIZATION_AND_CHILDREN -> appendOrganizationAndChildrenScope(scope, user);
            case CUSTOM ->
                    throw new PlatformException("custom data scope condition is not supported yet");
            case REFERENCE_DEPENDENCY ->
                    throw new PlatformException("reference dependency data scope is not supported yet");
            case NONE, ALL -> {
            }
        }
    }

    private void appendCurrentTenantScope(Criteria scope, CurrentUser user) {
        if (user.tenantId() == null) {
            scope.orGroup(denied().getRoot());
            return;
        }
        scope.orEq(StandardEntitySchema.TENANT_ID_FIELD, user.tenantId());
    }

    private Criteria currentTenantScoped(Criteria grantScope, CurrentUser user) {
        if (user.tenantId() == null) {
            return denied();
        }
        return Criteria.of()
                .eq(StandardEntitySchema.TENANT_ID_FIELD, user.tenantId())
                .andGroup(grantScope.getRoot());
    }

    private void appendOrganizationAndChildrenScope(Criteria scope, CurrentUser user) {
        if (user.organizationId() == null) {
            return;
        }
        OrganizationService service = organizationService.orElseThrow(() ->
                new PlatformException("organization children data scope requires organization hierarchy support"));
        List<String> organizationIds = service.organizationAndDescendantIds(user.organizationId());
        if (!organizationIds.isEmpty()) {
            scope.orIn(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, organizationIds);
        }
    }

    private DataScopePolicy normalizePolicy(RoleAction grant) {
        return grant.getDataScopePolicy() == null ? DataScopePolicy.NONE : grant.getDataScopePolicy();
    }

    private DataScopePolicy normalizePolicy(DataScopePolicy policy) {
        return policy == null ? DataScopePolicy.NONE : policy;
    }

    private ActionDefaultGrantPolicy normalizeDefaultPolicy(ActionDefaultGrantPolicy policy) {
        return policy == null ? ActionDefaultGrantPolicy.NONE : policy;
    }

    private boolean allowsCrossTenant(RoleAction grant) {
        return grant != null && grant.getTenantScopePolicy() == TenantScopePolicy.ALL_TENANTS;
    }

    private SqlRawCondition csvContains(String columnName, String paramName, String value) {
        return SqlRawCondition.of(
                "CONCAT(',', " + columnName + ", ',') LIKE :" + paramName,
                Map.of(paramName, "%," + value + ",%")
        );
    }

    private Criteria denied() {
        return Criteria.of().raw(SqlRawCondition.of("1 = 0", Map.of()));
    }

    private Criteria combine(Criteria base, Criteria scope) {
        if (scope == null || scope.isEmpty()) {
            return base;
        }
        if (base == null || base.isEmpty()) {
            return scope;
        }
        return Criteria.of()
                .andGroup(base.getRoot())
                .andGroup(scope.getRoot());
    }

    private record GrantScope(Criteria criteria, boolean allData, boolean crossTenant) {
        private GrantScope {
            criteria = criteria == null ? Criteria.of() : criteria;
        }

        static GrantScope all(boolean crossTenant) {
            return new GrantScope(Criteria.of(), true, crossTenant);
        }

        static GrantScope restricted(Criteria criteria, boolean crossTenant) {
            return new GrantScope(criteria, false, crossTenant);
        }

        static GrantScope none() {
            return new GrantScope(Criteria.of(), false, false);
        }

        boolean contributes() {
            return allData || !criteria.isEmpty();
        }
    }
}
