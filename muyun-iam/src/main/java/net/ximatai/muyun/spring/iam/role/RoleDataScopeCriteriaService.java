package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
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
        Criteria base = criteria == null ? Criteria.of() : criteria;
        CurrentUser user = currentUser.orElse(null);
        if (user == null) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        if (user.system()) {
            return DataScopeCriteriaResult.unrestricted(base);
        }
        List<RoleAction> grants = roleService.effectiveActionGrants(user.userId(), moduleAlias, actionCode);
        if (grants.isEmpty()) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        if (grants.stream().anyMatch(this::allowsAllData)) {
            return DataScopeCriteriaResult.unrestricted(base);
        }
        Criteria scope = Criteria.of();
        for (RoleAction grant : grants) {
            appendScope(scope, grant, user);
        }
        if (scope.isEmpty()) {
            return DataScopeCriteriaResult.restricted(combine(base, denied()));
        }
        return DataScopeCriteriaResult.restricted(combine(base, scope));
    }

    @Override
    public Criteria applyReadScope(String moduleAlias,
                                   String actionCode,
                                   Criteria criteria,
                                   Optional<CurrentUser> currentUser) {
        return resolveReadScope(moduleAlias, actionCode, criteria, currentUser).criteria();
    }

    private boolean allowsAllData(RoleAction grant) {
        DataScopePolicy policy = normalizePolicy(grant);
        return policy == DataScopePolicy.NONE || policy == DataScopePolicy.ALL;
    }

    private void appendScope(Criteria scope, RoleAction grant, CurrentUser user) {
        switch (normalizePolicy(grant)) {
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
}
