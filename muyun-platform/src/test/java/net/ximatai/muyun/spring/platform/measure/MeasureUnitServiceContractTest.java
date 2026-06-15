package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasureUnitServiceContractTest {
    private final TestMemoryDao<MeasureUnitCategory> categoryDao = new TestMemoryDao<>();
    private final TestMemoryDao<MeasureUnit> unitDao = new TestMemoryDao<>();
    private final MeasureUnitCategoryService categoryService = new MeasureUnitCategoryService(categoryDao);
    private final MeasureUnitService unitService = new MeasureUnitService(unitDao, categoryService);
    private final MeasureUnitConversionService conversionService =
            new MeasureUnitConversionService(categoryService, unitService);

    @Test
    void shouldCreateApplicationScopedMeasureUnitCategory() {
        String id = categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));

        MeasureUnitCategory loaded = categoryService.select(id);
        assertThat(loaded.getApplicationAlias()).isEqualTo("crm");
        assertThat(loaded.getAlias()).isEqualTo("weight");
        assertThat(loaded.getDimension()).isEqualTo(MeasureDimension.MASS);
        assertThat(loaded.getBaseUnitCode()).isEqualTo("g");
    }

    @Test
    void shouldRejectDuplicateCategoryAliasWithinApplication() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));

        assertThatThrownBy(() -> categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "kg")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unique");
        assertThat(categoryService.insert(category("sales", "weight", MeasureDimension.MASS, "g")))
                .isNotBlank();
    }

    @Test
    void shouldListCategoriesWithinApplicationScope() {
        categoryService.insert(category("crm", "quantity", MeasureDimension.COUNT, "bottle"));
        String lengthId = categoryService.insert(category("crm", "length", MeasureDimension.LENGTH, "m"));
        categoryService.insert(category("sales", "quantity", MeasureDimension.COUNT, "bottle"));
        categoryService.disable(lengthId);

        assertThat(categoryService.listCategories("crm", true))
                .extracting(MeasureUnitCategory::getAlias)
                .containsExactly("quantity");
        assertThat(categoryService.listCategories("crm", false))
                .extracting(MeasureUnitCategory::getAlias)
                .containsExactly("quantity", "length");
    }

    @Test
    void shouldRejectCategorySemanticIdentityChanges() {
        String id = categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));

        MeasureUnitCategory changedBase = category("crm", "weight", MeasureDimension.MASS, "kg");
        changedBase.setId(id);
        changedBase.setVersion(0);
        assertThatThrownBy(() -> categoryService.update(changedBase))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("base unit");

        MeasureUnitCategory changedAlias = category("crm", "length", MeasureDimension.MASS, "kg");
        changedAlias.setId(id);
        changedAlias.setVersion(0);
        assertThatThrownBy(() -> categoryService.update(changedAlias))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("alias");

        MeasureUnitCategory changedDimension = category("crm", "weight", MeasureDimension.LENGTH, "g");
        changedDimension.setId(id);
        changedDimension.setVersion(0);
        assertThatThrownBy(() -> categoryService.update(changedDimension))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dimension");
    }

    @Test
    void shouldCreateUnitsWithCodeAsBusinessValue() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        String gId = unitService.insert(unit("crm", "weight", "g", "g", 0, "1", "0"));
        unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));

        assertThat(unitService.resolveUnit("crm", "weight", "g").getId()).isEqualTo(gId);
        assertThat(unitService.listUnits("crm", "weight", true))
                .extracting(MeasureUnit::getCode)
                .containsExactly("g", "kg");
    }

    @Test
    void shouldRejectUnitDuplicateAndIdentityChanges() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        String id = unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));

        assertThatThrownBy(() -> unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unique");

        MeasureUnit changedCode = unit("crm", "weight", "g", "g", 0, "1", "0");
        changedCode.setId(id);
        changedCode.setVersion(0);
        assertThatThrownBy(() -> unitService.update(changedCode))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("code");
    }

    @Test
    void shouldValidateUnitShape() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));

        assertThatThrownBy(() -> unitService.insert(unit("crm", "weight", "kg", "kg", -1, "1000", "0")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("scale");
        assertThatThrownBy(() -> unitService.insert(unit("crm", "weight", "zero", "0", 0, "0", "0")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void shouldResolveOnlyEnabledUnitsForConversion() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        unitService.insert(unit("crm", "weight", "g", "g", 0, "1", "0"));
        String kgId = unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));
        unitService.disable(kgId);

        assertThatThrownBy(() -> conversionService.normalize("crm", "weight", BigDecimal.ONE, "kg"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("enabled unit");
    }

    @Test
    void shouldConvertWithinSameCategoryThroughBaseUnit() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        unitService.insert(unit("crm", "weight", "g", "g", 0, "1", "0"));
        unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));

        MeasureUnitConversion conversion =
                conversionService.convert("crm", "weight", new BigDecimal("2.5"), "kg", "g");

        assertThat(conversion.baseValue()).isEqualByComparingTo("2500");
        assertThat(conversion.convertedValue()).isEqualByComparingTo("2500");
        assertThat(conversion.fromUnitCode()).isEqualTo("kg");
        assertThat(conversion.toUnitCode()).isEqualTo("g");
    }

    @Test
    void shouldRequireConfiguredBaseUnitForConversion() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        unitService.insert(unit("crm", "weight", "g", "g", 0, "2", "0"));
        unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));

        assertThatThrownBy(() -> conversionService.convert("crm", "weight", BigDecimal.ONE, "kg", "g"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("base unit");
    }

    @Test
    void shouldApplyOffsetAndTargetScaleDuringConversion() {
        categoryService.insert(category("crm", "temperature", MeasureDimension.CUSTOM, "c"));
        unitService.insert(unit("crm", "temperature", "c", "C", 2, "1", "0"));
        unitService.insert(unit("crm", "temperature", "k", "K", 2, "1", "-273.15"));

        MeasureUnitConversion conversion =
                conversionService.convert("crm", "temperature", new BigDecimal("300"), "k", "c");

        assertThat(conversion.baseValue()).isEqualByComparingTo("26.85");
        assertThat(conversion.convertedValue()).isEqualByComparingTo("26.85");
    }

    @Test
    void shouldReorderUnitsWithinSameCategory() {
        categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
        String gId = unitService.insert(unit("crm", "weight", "g", "g", 0, "1", "0"));
        String kgId = unitService.insert(unit("crm", "weight", "kg", "kg", 3, "1000", "0"));

        unitService.reorder(List.of(kgId, gId));

        assertThat(unitService.listUnits("crm", "weight", true))
                .extracting(MeasureUnit::getCode)
                .containsExactly("kg", "g");
    }

    @Test
    void shouldIsolateMeasureUnitsByTenantScope() {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            categoryService.insert(category("crm", "weight", MeasureDimension.MASS, "g"));
            unitService.insert(unit("crm", "weight", "g", "g", 0, "1", "0"));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(unitService.resolveUnit("crm", "weight", "g")).isNull();
        }
    }

    private MeasureUnitCategory category(String applicationAlias,
                                         String alias,
                                         MeasureDimension dimension,
                                         String baseUnitCode) {
        MeasureUnitCategory category = new MeasureUnitCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setDimension(dimension);
        category.setBaseUnitCode(baseUnitCode);
        category.setTitle(alias);
        return category;
    }

    private MeasureUnit unit(String applicationAlias,
                             String categoryAlias,
                             String code,
                             String symbol,
                             Integer scale,
                             String factorToBase,
                             String offsetToBase) {
        MeasureUnit unit = new MeasureUnit();
        unit.setApplicationAlias(applicationAlias);
        unit.setCategoryAlias(categoryAlias);
        unit.setCode(code);
        unit.setTitle(code);
        unit.setSymbol(symbol);
        unit.setScale(scale);
        unit.setFactorToBase(new BigDecimal(factorToBase));
        unit.setOffsetToBase(new BigDecimal(offsetToBase));
        unit.setRoundingMode(RoundingMode.HALF_UP);
        return unit;
    }
}
