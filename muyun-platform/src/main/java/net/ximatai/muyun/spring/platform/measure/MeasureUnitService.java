package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class MeasureUnitService extends AbstractAbilityService<MeasureUnit> implements
        SoftDeleteAbility<MeasureUnit>,
        EnableAbility<MeasureUnit>,
        SortAbility<MeasureUnit>,
        ReferenceAbility<MeasureUnit>,
        CacheAbility<MeasureUnit> {
    public static final String MODULE_ALIAS = "platform.measure_unit";

    private final MeasureUnitCategoryService categoryService;

    public MeasureUnitService(BaseDao<MeasureUnit, String> unitDao,
                              MeasureUnitCategoryService categoryService) {
        super(MODULE_ALIAS, MeasureUnit.class, unitDao);
        this.categoryService = categoryService;
    }

    @Override
    public void beforeInsert(MeasureUnit unit) {
        normalizeAndValidate(unit);
    }

    @Override
    public void beforeUpdate(MeasureUnit unit) {
        normalizeAndValidate(unit);
        validateImmutableIdentity(unit);
    }

    @Override
    public Criteria sortScope(MeasureUnit unit) {
        return categoryScope(unit.getApplicationAlias(), unit.getCategoryAlias());
    }

    @Override
    public void validateSortScope(MeasureUnit left, MeasureUnit right) {
        validateSortScopeByFields(left, right,
                "Measure unit sort can only move records within the same category",
                "applicationAlias", "categoryAlias");
    }

    public MeasureUnit resolveUnit(String applicationAlias, String categoryAlias, String unitCode) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireCode(categoryAlias, "measureUnitCategoryAlias");
        String validUnitCode = requireCode(unitCode, "measureUnitCode");
        return findOne(categoryScope(validApplicationAlias, validCategoryAlias).eq("code", validUnitCode));
    }

    public MeasureUnit requireUnit(String applicationAlias, String categoryAlias, String unitCode) {
        MeasureUnit unit = resolveUnit(applicationAlias, categoryAlias, unitCode);
        if (unit == null) {
            throw new PlatformException("Measure unit requires existing unit: " + unitCode);
        }
        return unit;
    }

    public MeasureUnit requireEnabledUnit(String applicationAlias, String categoryAlias, String unitCode) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireCode(categoryAlias, "measureUnitCategoryAlias");
        categoryService.requireEnabledCategory(validApplicationAlias, validCategoryAlias);
        MeasureUnit unit = resolveUnit(validApplicationAlias, validCategoryAlias, unitCode);
        if (unit == null || !Boolean.TRUE.equals(unit.getEnabled())) {
            throw new PlatformException("Measure unit requires enabled unit: " + unitCode);
        }
        return unit;
    }

    public List<MeasureUnit> listUnits(String applicationAlias, String categoryAlias, boolean enabledOnly) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireCode(categoryAlias, "measureUnitCategoryAlias");
        if (enabledOnly) {
            categoryService.requireEnabledCategory(validApplicationAlias, validCategoryAlias);
        } else {
            categoryService.requireCategory(validApplicationAlias, validCategoryAlias);
        }
        Criteria criteria = categoryScope(validApplicationAlias, validCategoryAlias);
        if (enabledOnly) {
            criteria.eq("enabled", Boolean.TRUE);
        }
        return list(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private void normalizeAndValidate(MeasureUnit unit) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(unit.getApplicationAlias());
        String categoryAlias = requireCode(unit.getCategoryAlias(), "measureUnitCategoryAlias");
        MeasureUnitCategory category = categoryService.requireCategory(applicationAlias, categoryAlias);
        String code = requireCode(unit.getCode(), "measureUnitCode");
        unit.setApplicationAlias(category.getApplicationAlias());
        unit.setCategoryAlias(category.getAlias());
        unit.setCode(code);
        if (unit.getSymbol() != null && unit.getSymbol().isBlank()) {
            unit.setSymbol(null);
        }
        if (unit.getScale() != null && unit.getScale() < 0) {
            throw new PlatformException("measure unit scale must not be negative: " + unit.getCode());
        }
        if (unit.getFactorToBase() == null) {
            unit.setFactorToBase(BigDecimal.ONE);
        }
        if (unit.getFactorToBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PlatformException("measure unit factorToBase must be positive: " + unit.getCode());
        }
        if (unit.getOffsetToBase() == null) {
            unit.setOffsetToBase(BigDecimal.ZERO);
        }
        if (unit.getRoundingMode() == null) {
            unit.setRoundingMode(RoundingMode.HALF_UP);
        }
        rejectDuplicate(unit, categoryScope(unit.getApplicationAlias(), unit.getCategoryAlias())
                        .eq("code", unit.getCode()),
                "measure unit code must be unique within category: " + unit.getCode());
    }

    private void validateImmutableIdentity(MeasureUnit unit) {
        MeasureUnit existing = selectIncludingDeleted(unit.getId());
        rejectChanged(existing, unit, "Measure unit application", MeasureUnit::getApplicationAlias);
        rejectChanged(existing, unit, "Measure unit category", MeasureUnit::getCategoryAlias);
        rejectChanged(existing, unit, "Measure unit code", MeasureUnit::getCode);
    }

    private Criteria categoryScope(String applicationAlias, String categoryAlias) {
        return Criteria.of()
                .eq("applicationAlias", applicationAlias)
                .eq("categoryAlias", categoryAlias);
    }

    private String requireCode(String value, String name) {
        return PlatformNameRules.requireCode(value, name);
    }
}
