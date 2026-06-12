package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.builder.sql.SchemaBuildRules;
import net.ximatai.muyun.database.core.orm.CompiledCriteria;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
import net.ximatai.muyun.database.core.orm.SqlSubQuery;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopePlan;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeRequest;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class RoleDataScopeCriteriaService implements DataScopeCriteriaService {
    private final CriteriaSqlCompiler criteriaSqlCompiler = new CriteriaSqlCompiler();
    private final RoleService roleService;
    private final Optional<OrganizationService> organizationService;
    private final Optional<DepartmentService> departmentService;
    private final Optional<ReferenceDependencyScopeResolver> referenceDependencyScopeResolver;

    public RoleDataScopeCriteriaService(RoleService roleService) {
        this(roleService, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public RoleDataScopeCriteriaService(RoleService roleService, Optional<OrganizationService> organizationService) {
        this(roleService, organizationService, Optional.empty(), Optional.empty());
    }

    public RoleDataScopeCriteriaService(RoleService roleService,
                                        Optional<OrganizationService> organizationService,
                                        Optional<ReferenceDependencyScopeResolver> referenceDependencyScopeResolver) {
        this(roleService, organizationService, Optional.empty(), referenceDependencyScopeResolver);
    }

    @Autowired
    public RoleDataScopeCriteriaService(RoleService roleService,
                                        Optional<OrganizationService> organizationService,
                                        Optional<DepartmentService> departmentService,
                                        Optional<ReferenceDependencyScopeResolver> referenceDependencyScopeResolver) {
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
        this.organizationService = organizationService == null ? Optional.empty() : organizationService;
        this.departmentService = departmentService == null ? Optional.empty() : departmentService;
        this.referenceDependencyScopeResolver = referenceDependencyScopeResolver == null
                ? Optional.empty()
                : referenceDependencyScopeResolver;
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
        return resolveReadScope(moduleAlias, policy, criteria, currentUser, new HashSet<>());
    }

    private DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                     ActionExecutionPolicy policy,
                                                     Criteria criteria,
                                                     Optional<CurrentUser> currentUser,
                                                     Set<String> visiting) {
        Objects.requireNonNull(policy, "policy must not be null");
        Criteria base = criteria == null ? Criteria.of() : criteria;
        CurrentUser user = currentUser.orElse(null);
        if (user == null) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        if (user.system()) {
            return DataScopeCriteriaResult.unrestricted(base);
        }
        String visitKey = moduleAlias + ":" + policy.permissionActionCode();
        if (!visiting.add(visitKey)) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        List<EffectiveRoleActionGrant> grants = roleService.effectiveActionGrantsWithContext(
                user.userId(), moduleAlias, policy.permissionActionCode());
        try {
            List<GrantScope> scopes = grantScopes(moduleAlias, policy, user, grants, visiting);
            if (scopes.isEmpty()) {
                return DataScopeCriteriaResult.restricted(combine(base, denied()));
            }
            return combineGrantedScopes(base, user, scopes, grants.stream()
                    .map(EffectiveRoleActionGrant::actionGrant)
                    .anyMatch(this::allowsCrossTenant));
        } finally {
            visiting.remove(visitKey);
        }
    }

    private ActionExecutionPolicy policyOf(String actionCode) {
        Optional<PlatformAction> platformAction = PlatformAction.fromCode(actionCode);
        if (platformAction.isPresent()) {
            return platformAction.get().executionPolicy();
        }
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

    private List<GrantScope> grantScopes(String moduleAlias,
                                         ActionExecutionPolicy policy,
                                         CurrentUser user,
                                         List<EffectiveRoleActionGrant> grants,
                                         Set<String> visiting) {
        java.util.ArrayList<GrantScope> scopes = new java.util.ArrayList<>();
        GrantScope defaultScope = resolveDefaultScope(policy.defaultGrantPolicy(), user);
        if (defaultScope.contributes()) {
            scopes.add(defaultScope);
        }
        if (grants != null) {
            grants.stream()
                    .map(grant -> resolveGrantScope(moduleAlias, grant, user, visiting))
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

    private GrantScope resolveGrantScope(String moduleAlias,
                                         EffectiveRoleActionGrant effectiveGrant,
                                         CurrentUser user,
                                         Set<String> visiting) {
        RoleAction grant = effectiveGrant.actionGrant();
        DataScopePolicy policy = normalizePolicy(grant);
        if (policy == DataScopePolicy.ALL) {
            return GrantScope.all(allowsCrossTenant(grant));
        }
        if (policy == DataScopePolicy.WILDCARD) {
            return resolveWildcardScope(moduleAlias, grant, user, visiting);
        }
        if (policy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            return resolveReferenceDependencyScope(moduleAlias, grant, user, visiting);
        }
        Criteria criteria = criteriaForPolicy(policy, user, effectiveGrant.roleGrant());
        return GrantScope.restricted(criteria, allowsCrossTenant(grant));
    }

    private GrantScope resolveWildcardScope(String moduleAlias,
                                            RoleAction grant,
                                            CurrentUser user,
                                            Set<String> visiting) {
        RoleAction wildcardGrant = roleService.effectiveWildcardDataScopeGrant(user.userId(), grant.getActionCode());
        if (wildcardGrant == null) {
            return GrantScope.none();
        }
        DataScopePolicy wildcardPolicy = normalizePolicy(wildcardGrant);
        if (wildcardPolicy == DataScopePolicy.WILDCARD
                || wildcardPolicy == DataScopePolicy.CUSTOM
                || wildcardPolicy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            return GrantScope.none();
        }
        GrantScope resolved = resolveGrantScope(moduleAlias, new EffectiveRoleActionGrant(wildcardGrant, null), user, visiting);
        if (!resolved.contributes()) {
            return GrantScope.none();
        }
        return resolved.crossTenant() && !allowsCrossTenant(grant)
                ? new GrantScope(resolved.criteria(), resolved.allData(), false)
                : resolved;
    }

    private GrantScope resolveReferenceDependencyScope(String moduleAlias,
                                                       RoleAction grant,
                                                       CurrentUser user,
                                                       Set<String> visiting) {
        String referenceActionCode = normalizeReferenceActionCode(grant);
        ReferenceDependencyScopePlan plan = referenceDependencyScopeResolver
                .flatMap(resolver -> resolver.resolve(new ReferenceDependencyScopeRequest(
                        moduleAlias, grant.getReferenceFieldId(), referenceActionCode)))
                .orElse(null);
        if (plan == null) {
            return GrantScope.none();
        }
        DataScopeCriteriaResult targetScope = resolveReadScope(
                plan.targetModuleAlias(),
                policyOf(referenceActionCode),
                Criteria.of(),
                Optional.of(user),
                visiting
        );
        Criteria targetCriteria = targetScope.criteria();
        if (!targetScope.crossTenant()) {
            if (user.tenantId() == null) {
                return GrantScope.none();
            }
            targetCriteria = combine(targetCriteria, Criteria.of().eq(StandardEntitySchema.TENANT_ID_FIELD, user.tenantId()));
        }
        targetCriteria = combine(targetCriteria, Criteria.of().eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE));
        CompiledCriteria compiled = criteriaSqlCompiler.compile(targetCriteria,
                plan::resolveTargetColumn, plan.databaseType());
        String subQuery = "SELECT " + quote(plan.resolveTargetColumn(StandardEntitySchema.ID_FIELD), plan.databaseType())
                + " FROM " + SchemaBuildRules.qualifiedName(plan.targetSchemaName(), plan.targetTableName(), plan.databaseType())
                + where(compiled);
        boolean crossTenant = allowsCrossTenant(grant) && targetScope.crossTenant();
        return GrantScope.restricted(Criteria.of().inSubQuery(
                plan.sourceField(), SqlSubQuery.of(subQuery, compiled.getParams())), crossTenant);
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
        return criteriaForPolicy(policy, user, null);
    }

    private Criteria criteriaForPolicy(DataScopePolicy policy, CurrentUser user, EffectiveRoleGrant roleGrant) {
        return criteriaForPolicies(user, roleGrant, policy);
    }

    private Criteria criteriaForPolicies(CurrentUser user, DataScopePolicy... policies) {
        return criteriaForPolicies(user, null, policies);
    }

    private Criteria criteriaForPolicies(CurrentUser user, EffectiveRoleGrant roleGrant, DataScopePolicy... policies) {
        Criteria scope = Criteria.of();
        if (policies != null) {
            for (DataScopePolicy policy : policies) {
                appendScope(scope, policy, user, roleGrant);
            }
        }
        return scope;
    }

    private void appendScope(Criteria scope, DataScopePolicy policy, CurrentUser user, EffectiveRoleGrant roleGrant) {
        switch (normalizePolicy(policy)) {
            case OWNER -> scope.orEq(PlatformAbilityFields.AUTH_USER_FIELD, user.userId());
            case ASSIGNEE -> scope.orRaw(csvContains(PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN, "userId", user.userId()));
            case MEMBER -> scope.orRaw(csvContains(PlatformAbilityFields.AUTH_MEMBER_COLUMN, "userId", user.userId()));
            case ORGANIZATION -> {
                String organizationId = scopeOrganizationId(user, roleGrant);
                if (organizationId != null) {
                    scope.orEq(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, organizationId);
                }
            }
            case ORGANIZATION_AND_CHILDREN -> appendOrganizationAndChildrenScope(scope, user, roleGrant);
            case DEPARTMENT -> {
                String departmentId = scopeDepartmentId(roleGrant);
                if (departmentId != null) {
                    scope.orEq(PlatformAbilityFields.AUTH_DEPARTMENT_FIELD, departmentId);
                }
            }
            case DEPARTMENT_AND_CHILDREN -> appendDepartmentAndChildrenScope(scope, roleGrant);
            case CUSTOM ->
                    throw new PlatformException("custom data scope condition is not supported yet");
            case WILDCARD -> throw new PlatformException("wildcard data scope must be resolved before append scope");
            case REFERENCE_DEPENDENCY -> {
            }
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

    private void appendOrganizationAndChildrenScope(Criteria scope, CurrentUser user, EffectiveRoleGrant roleGrant) {
        String organizationId = scopeOrganizationId(user, roleGrant);
        if (organizationId == null) {
            return;
        }
        OrganizationService service = organizationService.orElseThrow(() ->
                new PlatformException("organization children data scope requires organization hierarchy support"));
        List<String> organizationIds = service.selfAndDescendantIds(organizationId);
        if (!organizationIds.isEmpty()) {
            scope.orIn(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, organizationIds);
        }
    }

    private void appendDepartmentAndChildrenScope(Criteria scope, EffectiveRoleGrant roleGrant) {
        String organizationId = scopeOrganizationId(null, roleGrant);
        String departmentId = scopeDepartmentId(roleGrant);
        if (organizationId == null || departmentId == null) {
            return;
        }
        DepartmentService service = departmentService.orElseThrow(() ->
                new PlatformException("department children data scope requires department hierarchy support"));
        List<String> departmentIds = service.selfAndDescendantIds(organizationId, departmentId);
        if (!departmentIds.isEmpty()) {
            scope.orIn(PlatformAbilityFields.AUTH_DEPARTMENT_FIELD, departmentIds);
        }
    }

    private String scopeOrganizationId(CurrentUser user, EffectiveRoleGrant roleGrant) {
        String contextOrganizationId = roleGrant == null ? null : roleGrant.organizationId();
        if (contextOrganizationId != null && !contextOrganizationId.isBlank()) {
            return contextOrganizationId;
        }
        return user == null ? null : user.organizationId();
    }

    private String scopeDepartmentId(EffectiveRoleGrant roleGrant) {
        String contextDepartmentId = roleGrant == null ? null : roleGrant.departmentId();
        return contextDepartmentId == null || contextDepartmentId.isBlank() ? null : contextDepartmentId;
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

    private String normalizeReferenceActionCode(RoleAction grant) {
        String actionCode = grant == null ? null : grant.getReferenceActionCode();
        return actionCode == null || actionCode.isBlank() ? PlatformAction.REFERENCE.code() : actionCode.trim();
    }

    private String where(CompiledCriteria criteria) {
        String sql = criteria == null ? "" : criteria.getSql();
        return sql == null || sql.isBlank() ? "" : " WHERE " + sql;
    }

    private String quote(String identifier, DBInfo.Type databaseType) {
        return SchemaBuildRules.quoteIdentifier(identifier, databaseType);
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
