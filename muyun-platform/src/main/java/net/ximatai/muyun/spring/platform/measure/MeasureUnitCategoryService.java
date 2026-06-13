package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class MeasureUnitCategoryService extends AbstractAbilityService<MeasureUnitCategory> implements
        SoftDeleteAbility<MeasureUnitCategory>,
        EnableAbility<MeasureUnitCategory>,
        SortAbility<MeasureUnitCategory>,
        ReferenceAbility<MeasureUnitCategory>,
        CacheAbility<MeasureUnitCategory> {
    public static final String MODULE_ALIAS = "platform.measure_unit_category";

    public MeasureUnitCategoryService(BaseDao<MeasureUnitCategory, String> categoryDao) {
        super(MODULE_ALIAS, MeasureUnitCategory.class, categoryDao);
    }

    @Override
    public void beforeInsert(MeasureUnitCategory category) {
        normalizeAndValidate(category);
    }

    @Override
    public void beforeUpdate(MeasureUnitCategory category) {
        normalizeAndValidate(category);
        validateImmutableIdentity(category);
    }

    @Override
    public Criteria sortScope(MeasureUnitCategory category) {
        return Criteria.of().eq("applicationAlias", category.getApplicationAlias());
    }

    @Override
    public void validateSortScope(MeasureUnitCategory left, MeasureUnitCategory right) {
        validateSortScopeByFields(left, right,
                "Measure unit category sort can only move records within the same application", "applicationAlias");
    }

    public MeasureUnitCategory requireCategory(String applicationAlias, String categoryAlias) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireAlias(categoryAlias);
        MeasureUnitCategory category = findOne(Criteria.of()
                .eq("applicationAlias", validApplicationAlias)
                .eq("alias", validCategoryAlias));
        if (category == null) {
            throw new PlatformException("Measure unit category requires existing category: " + validCategoryAlias);
        }
        return category;
    }

    public MeasureUnitCategory requireEnabledCategory(String applicationAlias, String categoryAlias) {
        MeasureUnitCategory category = requireCategory(applicationAlias, categoryAlias);
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new PlatformException("Measure unit category is disabled: " + categoryAlias);
        }
        return category;
    }

    private void normalizeAndValidate(MeasureUnitCategory category) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(category.getApplicationAlias());
        String alias = requireAlias(category.getAlias());
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        if (category.getDimension() == null) {
            category.setDimension(MeasureDimension.CUSTOM);
        }
        if (category.getBaseUnitCode() != null && !category.getBaseUnitCode().isBlank()) {
            category.setBaseUnitCode(requireCode(category.getBaseUnitCode(), "baseUnitCode"));
        } else {
            category.setBaseUnitCode(null);
        }
        rejectDuplicate(category, Criteria.of()
                        .eq("applicationAlias", category.getApplicationAlias())
                        .eq("alias", category.getAlias()),
                "measureUnitCategoryAlias must be unique within application: " + category.getAlias());
    }

    private void validateImmutableIdentity(MeasureUnitCategory category) {
        MeasureUnitCategory existing = selectIncludingDeleted(category.getId());
        rejectChanged(existing, category, "Measure unit category application", MeasureUnitCategory::getApplicationAlias);
        rejectChanged(existing, category, "Measure unit category alias", MeasureUnitCategory::getAlias);
        rejectChanged(existing, category, "Measure unit category dimension", MeasureUnitCategory::getDimension);
        rejectChanged(existing, category, "Measure unit category base unit", MeasureUnitCategory::getBaseUnitCode);
    }

    private String requireAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "measureUnitCategoryAlias");
    }

    private String requireCode(String value, String name) {
        return PlatformNameRules.requireCode(value, name);
    }
}
