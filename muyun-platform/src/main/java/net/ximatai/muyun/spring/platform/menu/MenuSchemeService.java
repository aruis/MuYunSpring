package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.OrganizationHierarchyService;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MenuSchemeService extends AbstractAbilityService<MenuScheme> implements
        SoftDeleteAbility<MenuScheme>,
        EnableAbility<MenuScheme>,
        SortAbility<MenuScheme> {
    public static final String MODULE_ALIAS = "platform.menu_scheme";
    public static final String SYSTEM_SCOPE_ID = "system";
    private final Optional<OrganizationHierarchyService> organizationHierarchyService;

    public MenuSchemeService(BaseDao<MenuScheme, String> schemeDao) {
        this(schemeDao, Optional.empty());
    }

    @Autowired
    public MenuSchemeService(BaseDao<MenuScheme, String> schemeDao,
                             Optional<OrganizationHierarchyService> organizationHierarchyService) {
        super(MODULE_ALIAS, MenuScheme.class, schemeDao);
        this.organizationHierarchyService = organizationHierarchyService == null
                ? Optional.empty()
                : organizationHierarchyService;
    }

    @Override
    public void beforeInsert(MenuScheme scheme) {
        normalizeAndValidate(scheme);
    }

    @Override
    public void beforeUpdate(MenuScheme scheme) {
        validateImmutableIdentity(scheme);
        normalizeAndValidate(scheme);
    }

    @Override
    public Criteria sortScope(MenuScheme scheme) {
        return Criteria.of()
                .eq(StandardEntitySchema.TENANT_ID_FIELD, scheme.getTenantId())
                .eq("scopeType", scheme.getScopeType())
                .eq("scopeId", scheme.getScopeId());
    }

    @Override
    public void validateSortScope(MenuScheme left, MenuScheme right) {
        if (!Objects.equals(left.getTenantId(), right.getTenantId())
                || left.getScopeType() != right.getScopeType()
                || !Objects.equals(left.getScopeId(), right.getScopeId())) {
            throw new PlatformException("Menu scheme sort can only move records within the same scope");
        }
    }

    private void normalizeAndValidate(MenuScheme scheme) {
        scheme.setAlias(requireAlias(scheme.getAlias()));
        if (scheme.getScopeType() == null) {
            scheme.setScopeType(MenuScopeType.TENANT);
        }
        normalizeScope(scheme);
        rejectDuplicateAlias(scheme);
    }

    private String requireAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "menuSchemeAlias");
    }

    private void normalizeScope(MenuScheme scheme) {
        switch (scheme.getScopeType()) {
            case SYSTEM -> {
                if (!TenantContext.isSystem()) {
                    throw new PlatformException("System menu scheme requires system context");
                }
                scheme.setTenantId(null);
                scheme.setScopeId(SYSTEM_SCOPE_ID);
            }
            case TENANT -> {
                if (scheme.getTenantId() == null || scheme.getTenantId().isBlank()) {
                    throw new PlatformException("Tenant menu scheme requires tenantId");
                }
                if (scheme.getScopeId() == null || scheme.getScopeId().isBlank()) {
                    scheme.setScopeId(scheme.getTenantId());
                }
            }
            case ORGANIZATION -> {
                if (scheme.getTenantId() == null || scheme.getTenantId().isBlank()) {
                    throw new PlatformException("Organization menu scheme requires tenantId");
                }
                if (scheme.getScopeId() == null || scheme.getScopeId().isBlank()) {
                    throw new PlatformException("Organization menu scheme requires scopeId");
                }
            }
        }
    }

    private void rejectDuplicateAlias(MenuScheme scheme) {
        rejectDuplicate(scheme, Criteria.of()
                        .eq(StandardEntitySchema.TENANT_ID_FIELD, scheme.getTenantId())
                        .eq("scopeType", scheme.getScopeType())
                        .eq("scopeId", scheme.getScopeId())
                        .eq("alias", scheme.getAlias()),
                "menuSchemeAlias must be unique within scope: " + scheme.getAlias());
    }

    private void validateImmutableIdentity(MenuScheme scheme) {
        MenuScheme existing = selectIgnoreSoftDelete(scheme.getId());
        if (existing == null) {
            return;
        }
        boolean changed = !Objects.equals(existing.getAlias(), scheme.getAlias())
                || existing.getScopeType() != scheme.getScopeType()
                || !Objects.equals(existing.getScopeId(), effectiveScopeId(scheme))
                || !Objects.equals(existing.getTenantId(), scheme.getTenantId());
        if (changed) {
            throw new PlatformException("Menu scheme identity cannot be changed");
        }
    }

    private String effectiveScopeId(MenuScheme scheme) {
        if (scheme.getScopeType() == MenuScopeType.SYSTEM) {
            return SYSTEM_SCOPE_ID;
        }
        if (scheme.getScopeType() == MenuScopeType.TENANT
                && (scheme.getScopeId() == null || scheme.getScopeId().isBlank())) {
            return scheme.getTenantId();
        }
        return scheme.getScopeId();
    }

    public MenuScheme resolveCurrentUserScheme(CurrentUser user) {
        if (user == null) {
            throw new PlatformException("current user is required");
        }
        if (user.system()) {
            return requireFirstEnabledScheme(MenuScopeType.SYSTEM, null, SYSTEM_SCOPE_ID);
        }
        if (user.tenantId() == null || user.tenantId().isBlank()) {
            throw new PlatformException("current user tenant is required");
        }
        if (user.organizationId() != null && !user.organizationId().isBlank()) {
            MenuScheme organizationScheme = firstOrganizationScheme(user.tenantId(), user.organizationId());
            if (organizationScheme != null) {
                return organizationScheme;
            }
        }
        return requireFirstEnabledScheme(MenuScopeType.TENANT, user.tenantId(), user.tenantId());
    }

    private MenuScheme firstOrganizationScheme(String tenantId, String organizationId) {
        for (String candidateId : organizationCandidateIds(organizationId)) {
            MenuScheme scheme = firstEnabledScheme(MenuScopeType.ORGANIZATION, tenantId, candidateId);
            if (scheme != null) {
                return scheme;
            }
        }
        return null;
    }

    private List<String> organizationCandidateIds(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return List.of();
        }
        return organizationHierarchyService
                .map(service -> service.organizationIdsFromSelfToRoot(organizationId))
                .filter(ids -> ids != null && !ids.isEmpty())
                .orElseGet(() -> List.of(organizationId));
    }

    private MenuScheme requireFirstEnabledScheme(MenuScopeType scopeType, String tenantId, String scopeId) {
        MenuScheme scheme = firstEnabledScheme(scopeType, tenantId, scopeId);
        if (scheme == null) {
            throw new PlatformException("menu scheme is not configured for current user");
        }
        return scheme;
    }

    private MenuScheme firstEnabledScheme(MenuScopeType scopeType, String tenantId, String scopeId) {
        List<MenuScheme> schemes = list(Criteria.of()
                        .eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId)
                        .eq("scopeType", scopeType)
                        .eq("scopeId", scopeId)
                        .eq("enabled", Boolean.TRUE),
                PageRequest.of(1, 1),
                Sort.asc("sortOrder"));
        return schemes.isEmpty() ? null : schemes.getFirst();
    }
}
