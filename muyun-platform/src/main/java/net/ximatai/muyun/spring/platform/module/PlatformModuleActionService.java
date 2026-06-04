package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PlatformModuleActionService extends AbstractAbilityService<PlatformModuleAction> implements
        SoftDeleteAbility<PlatformModuleAction>,
        EnableAbility<PlatformModuleAction>,
        SortAbility<PlatformModuleAction> {
    public static final String MODULE_ALIAS = "platform.module_action";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;

    public PlatformModuleActionService(BaseDao<PlatformModuleAction, String> actionDao,
                                       PlatformModuleService moduleService) {
        super(MODULE_ALIAS, PlatformModuleAction.class, actionDao);
        this.moduleService = moduleService;
    }

    @Override
    public void beforeInsert(PlatformModuleAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public void beforeUpdate(PlatformModuleAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public Criteria sortScope(PlatformModuleAction action) {
        return Criteria.of().eq("moduleAlias", action.getModuleAlias());
    }

    @Override
    public void validateSortScope(PlatformModuleAction left, PlatformModuleAction right) {
        if (!Objects.equals(left.getModuleAlias(), right.getModuleAlias())) {
            throw new PlatformException("Module action sort can only move records within the same module");
        }
    }

    public List<PlatformModuleAction> listByModuleAliases(List<String> moduleAliases) {
        if (moduleAliases == null || moduleAliases.isEmpty()) {
            return List.of();
        }
        try (TenantContext.Scope ignored = TenantContext.system("select global module actions")) {
            return list(Criteria.of()
                    .in("moduleAlias", moduleAliases)
                    .isNull(StandardEntitySchema.TENANT_ID_FIELD), ALL, Sort.asc("sortOrder"));
        }
    }

    public PlatformModuleAction findByModuleAliasAndActionCode(String moduleAlias, String actionCode) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        String validActionCode = PlatformNameRules.requireActionCode(actionCode, "actionCode");
        try (TenantContext.Scope ignored = TenantContext.system("select global module action")) {
            return findOne(Criteria.of()
                    .eq("moduleAlias", validModuleAlias)
                    .eq("actionCode", validActionCode)
                    .isNull(StandardEntitySchema.TENANT_ID_FIELD));
        }
    }

    private void normalizeAndValidate(PlatformModuleAction action) {
        String moduleAlias = PlatformNameRules.requireModuleAlias(action.getModuleAlias());
        if (moduleService.resolveVisibleModule(moduleAlias) == null) {
            throw new PlatformException("Module action requires existing module: " + moduleAlias);
        }
        action.setModuleAlias(moduleAlias);
        action.setActionCode(PlatformNameRules.requireActionCode(action.getActionCode(), "actionCode"));
        if (action.getPermissionActionCode() != null && action.getPermissionActionCode().isBlank()) {
            action.setPermissionActionCode(null);
        }
        if (action.getPermissionActionCode() != null) {
            action.setPermissionActionCode(PlatformNameRules.requireActionCode(
                    action.getPermissionActionCode(), "permissionActionCode"));
        }
        if (action.getTitle() == null || action.getTitle().isBlank()) {
            action.setTitle(action.getActionCode());
        }
        if (action.getActionLevel() == null) {
            action.setActionLevel(EntityActionLevel.ANY);
        }
        if (action.getAccessMode() == null) {
            action.setAccessMode(EntityActionAccessMode.AUTH_REQUIRED);
        }
        if (action.getActionAuth() == null) {
            action.setActionAuth(action.getAccessMode() == EntityActionAccessMode.AUTH_REQUIRED);
        }
        if (action.getDataAuth() == null) {
            action.setDataAuth(false);
        }
        if (action.getSystemManaged() == null) {
            action.setSystemManaged(false);
        }
        action.setTenantId(null);
        rejectDuplicate(action, Criteria.of()
                        .eq("moduleAlias", action.getModuleAlias())
                        .eq("actionCode", action.getActionCode())
                        .isNull(StandardEntitySchema.TENANT_ID_FIELD),
                "module action must be unique in module: " + action.getModuleAlias() + "." + action.getActionCode());
    }
}
