package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class PlatformModuleService extends AbstractAbilityService<PlatformModule> implements
        SoftDeleteAbility<PlatformModule>,
        EnableAbility<PlatformModule>,
        TreeAbility<PlatformModule> {

    public static final String MODULE_ALIAS = "platform.module";

    public PlatformModuleService(BaseDao<PlatformModule, String> moduleDao) {
        super(MODULE_ALIAS, PlatformModule.class, moduleDao);
    }

    @Override
    public void beforePrepareInsert(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public void beforeInsert(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public void beforeUpdate(PlatformModule module) {
        normalizeAndValidate(module);
    }

    @Override
    public Criteria sortScope(PlatformModule module) {
        return scopedTreeCriteria(module, "applicationAlias");
    }

    @Override
    public void validateSortScope(PlatformModule left, PlatformModule right) {
        validateTreeSortScopeByFields(left, right,
                "Module sort can only move records within the same application", "applicationAlias");
    }

    @Override
    public List<PlatformModule> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootModules(applicationAlias)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<PlatformModule> rootModules(String applicationAlias) {
        return children(applicationAlias, TreeAbility.ROOT_ID);
    }

    public List<PlatformModule> children(String applicationAlias, String parentId) {
        return TreeAbility.super.children(applicationScope(PlatformNameRules.requireApplicationAlias(applicationAlias)), parentId);
    }

    public PlatformModule resolveVisibleModule(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        if (TenantContext.currentTenantId().isPresent()) {
            PlatformModule scoped = select(validAlias);
            if (scoped != null) {
                return scoped;
            }
        }
        return selectGlobalModule(validAlias);
    }

    private PlatformModule selectGlobalModule(String moduleAlias) {
        try (TenantContext.Scope ignored = TenantContext.system("select global platform module")) {
            return getDao().query(activeCriteria(Criteria.of()
                            .eq("id", moduleAlias)),
                    new PageRequest(0, 1))
                    .stream()
                    .filter(module -> module.getTenantId() == null || module.getTenantId().isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }

    private void normalizeAndValidate(PlatformModule module) {
        String applicationAlias = requireApplicationAlias(module.getApplicationAlias());
        String moduleAlias = requireModuleAlias(module.getAlias(), applicationAlias);
        module.setApplicationAlias(applicationAlias);
        module.setAlias(moduleAlias);
        if (module.getModuleKind() == null) {
            module.setModuleKind(ModuleKind.STATIC);
        }
        validateParentApplication(module);
    }

    private String requireApplicationAlias(String applicationAlias) {
        return PlatformNameRules.requireApplicationAlias(applicationAlias);
    }

    private String requireModuleAlias(String moduleAlias, String applicationAlias) {
        return PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
    }

    private void validateParentApplication(PlatformModule module) {
        validateTreePlacementInScope(module, applicationScope(module.getApplicationAlias()),
                "Module parent must belong to the same application");
    }

    private Criteria applicationScope(String applicationAlias) {
        return Criteria.of().eq("applicationAlias", applicationAlias);
    }
}
