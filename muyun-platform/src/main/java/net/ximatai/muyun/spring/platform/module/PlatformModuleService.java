package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
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
        return Criteria.of()
                .eq("applicationAlias", module.getApplicationAlias())
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, module.getParentId());
    }

    @Override
    public void validateSortScope(PlatformModule left, PlatformModule right) {
        if (!java.util.Objects.equals(left.getApplicationAlias(), right.getApplicationAlias())) {
            throw new AbilityException("Module sort can only move records within the same application");
        }
        TreeAbility.super.validateSortScope(left, right);
    }

    @Override
    public List<PlatformModule> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            throw new AbilityException("Use rootModules(applicationAlias) to resolve application-scoped root modules");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<PlatformModule> rootModules(String applicationAlias) {
        return children(applicationAlias, TreeAbility.ROOT_ID);
    }

    public List<PlatformModule> children(String applicationAlias, String parentId) {
        PlatformAliasRules.requireApplicationAlias(applicationAlias);
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!TreeAbility.ROOT_ID.equals(parentId)) {
            PlatformModule parent = selectActiveRaw(parentId);
            if (parent == null || !applicationAlias.equals(parent.getApplicationAlias())) {
                return List.of();
            }
        }
        Criteria criteria = activeCriteria(Criteria.of()
                .eq("applicationAlias", applicationAlias)
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
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
        return PlatformAliasRules.requireApplicationAlias(applicationAlias);
    }

    private String requireModuleAlias(String moduleAlias, String applicationAlias) {
        return PlatformAliasRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
    }

    private void validateParentApplication(PlatformModule module) {
        String parentId = module.getParentId();
        if (parentId == null || parentId.isBlank() || TreeAbility.ROOT_ID.equals(parentId)) {
            return;
        }
        PlatformModule parent = select(parentId);
        if (parent == null) {
            return;
        }
        if (!module.getApplicationAlias().equals(parent.getApplicationAlias())) {
            throw new AbilityException("Module parent must belong to the same application");
        }
    }
}
