package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PlatformUiSetService extends AbstractAbilityService<PlatformUiSet> implements
        SoftDeleteAbility<PlatformUiSet>,
        EnableAbility<PlatformUiSet>,
        SortAbility<PlatformUiSet> {
    public static final String MODULE_ALIAS = "platform.ui_set";

    private final PlatformModuleService moduleService;

    public PlatformUiSetService(BaseDao<PlatformUiSet, String> uiSetDao,
                                PlatformModuleService moduleService) {
        super(MODULE_ALIAS, PlatformUiSet.class, uiSetDao);
        this.moduleService = moduleService;
    }

    @Override
    public void beforeInsert(PlatformUiSet uiSet) {
        normalizeAndValidate(uiSet);
    }

    @Override
    public void beforeUpdate(PlatformUiSet uiSet) {
        normalizeAndValidate(uiSet);
        PlatformUiSet existing = selectIncludingDeleted(uiSet.getId());
        rejectChanged(existing, uiSet, "UI set moduleAlias", PlatformUiSet::getModuleAlias);
        rejectChanged(existing, uiSet, "UI set alias", PlatformUiSet::getAlias);
    }

    @Override
    public Criteria sortScope(PlatformUiSet uiSet) {
        return Criteria.of().eq("moduleAlias", uiSet.getModuleAlias());
    }

    @Override
    public void validateSortScope(PlatformUiSet left, PlatformUiSet right) {
        if (!Objects.equals(left.getModuleAlias(), right.getModuleAlias())) {
            throw new PlatformException("UI set sort can only move records within the same module");
        }
    }

    public PlatformUiSet requireUiSet(String id) {
        PlatformUiSet uiSet = id == null || id.isBlank() ? null : select(id);
        if (uiSet == null) {
            throw new PlatformException("UI set requires existing config: " + id);
        }
        return uiSet;
    }

    private void normalizeAndValidate(PlatformUiSet uiSet) {
        String moduleAlias = PlatformNameRules.requireModuleAlias(uiSet.getModuleAlias());
        PlatformModule module = moduleService.resolveVisibleModule(moduleAlias);
        if (module == null) {
            throw new PlatformException("UI set requires existing module: " + moduleAlias);
        }
        String alias = PlatformNameRules.requireIdentifier(uiSet.getAlias(), "uiSetAlias");
        uiSet.setModuleAlias(moduleAlias);
        uiSet.setAlias(alias);
        if (uiSet.getSetType() == null) {
            throw new PlatformException("UI set type must not be null");
        }
        if (uiSet.getTitle() == null || uiSet.getTitle().isBlank()) {
            uiSet.setTitle(alias);
        }
        if (uiSet.getDefaultSet() == null) {
            uiSet.setDefaultSet(Boolean.FALSE);
        }
        rejectDuplicate(uiSet, Criteria.of()
                        .eq("moduleAlias", moduleAlias)
                        .eq("alias", alias),
                "UI set alias must be unique in module: " + moduleAlias + "." + alias);
        if (Boolean.TRUE.equals(uiSet.getDefaultSet())) {
            rejectDuplicate(uiSet, Criteria.of()
                            .eq("moduleAlias", moduleAlias)
                            .eq("setType", uiSet.getSetType())
                            .eq("defaultSet", Boolean.TRUE),
                    "Only one default UI set is allowed for module and type: "
                            + moduleAlias + "." + uiSet.getSetType());
        }
    }
}
