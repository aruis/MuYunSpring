package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MenuSchemeService extends AbstractAbilityService<MenuScheme> implements
        SoftDeleteAbility<MenuScheme>,
        EnableAbility<MenuScheme>,
        SortAbility<MenuScheme> {
    public static final String MODULE_ALIAS = "platform.menuScheme";
    public static final String SYSTEM_SCOPE_ID = "system";

    public MenuSchemeService(BaseDao<MenuScheme, String> schemeDao) {
        super(MODULE_ALIAS, MenuScheme.class, schemeDao);
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
            throw new AbilityException("Menu scheme sort can only move records within the same scope");
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
        if (!PlatformAliasRules.isIdentifier(alias)) {
            throw new IllegalArgumentException("invalid menuSchemeAlias: " + alias);
        }
        return alias;
    }

    private void normalizeScope(MenuScheme scheme) {
        switch (scheme.getScopeType()) {
            case SYSTEM -> {
                if (!TenantContext.isSystem()) {
                    throw new AbilityException("System menu scheme requires system context");
                }
                scheme.setTenantId(null);
                scheme.setScopeId(SYSTEM_SCOPE_ID);
            }
            case TENANT -> {
                if (scheme.getTenantId() == null || scheme.getTenantId().isBlank()) {
                    throw new AbilityException("Tenant menu scheme requires tenantId");
                }
                if (scheme.getScopeId() == null || scheme.getScopeId().isBlank()) {
                    scheme.setScopeId(scheme.getTenantId());
                }
            }
            case ORGANIZATION -> {
                if (scheme.getTenantId() == null || scheme.getTenantId().isBlank()) {
                    throw new AbilityException("Organization menu scheme requires tenantId");
                }
                if (scheme.getScopeId() == null || scheme.getScopeId().isBlank()) {
                    throw new AbilityException("Organization menu scheme requires scopeId");
                }
            }
        }
    }

    private void rejectDuplicateAlias(MenuScheme scheme) {
        boolean duplicate = list(Criteria.of()
                .eq(StandardEntitySchema.TENANT_ID_FIELD, scheme.getTenantId())
                .eq("scopeType", scheme.getScopeType())
                .eq("scopeId", scheme.getScopeId())
                .eq("alias", scheme.getAlias()), PageRequest.of(1, Integer.MAX_VALUE))
                .stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), scheme.getId()));
        if (duplicate) {
            throw new AbilityException("menuSchemeAlias must be unique within scope: " + scheme.getAlias());
        }
    }

    private void validateImmutableIdentity(MenuScheme scheme) {
        MenuScheme existing = selectIgnoreSoftDelete(scheme.getId());
        if (existing == null) {
            return;
        }
        if (!Objects.equals(existing.getAlias(), scheme.getAlias())
                || existing.getScopeType() != scheme.getScopeType()
                || !Objects.equals(existing.getScopeId(), effectiveScopeId(scheme))
                || !Objects.equals(existing.getTenantId(), scheme.getTenantId())) {
            throw new AbilityException("Menu scheme identity cannot be changed");
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
}
