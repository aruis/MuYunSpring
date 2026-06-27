package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.measure.MeasureDimension;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeasureUnitWebControllerTest {
    @Test
    void shouldForceCategoryApplicationAliasFromPathOnInsert() throws Exception {
        MeasureUnitCategoryService service = mock(MeasureUnitCategoryService.class);
        MeasureUnitCategoryWebController controller = new MeasureUnitCategoryWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnitCategory inserted = category("crm", "weight");
        inserted.setId("cat-1");
        when(service.insert(any(MeasureUnitCategory.class))).thenReturn("cat-1");
        when(service.select("cat-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.application/crm/measure-unit-categories/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"other","alias":"weight","title":"Weight","dimension":"MASS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("crm"))
                .andExpect(jsonPath("$.record.alias").value("weight"));

        ArgumentCaptor<MeasureUnitCategory> captor = ArgumentCaptor.forClass(MeasureUnitCategory.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("crm");
    }

    @Test
    void shouldExposeCategoryOptionsWithinPathApplication() throws Exception {
        MeasureUnitCategoryService service = mock(MeasureUnitCategoryService.class);
        MeasureUnitCategoryWebController controller = new MeasureUnitCategoryWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.listVisibleCategories("crm", true)).thenReturn(List.of(category("platform", "quantity")));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/platform.application/crm/measure-unit-categories/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].applicationAlias").value("platform"))
                .andExpect(jsonPath("$.records[0].alias").value("quantity"));

        verify(service).listVisibleCategories("crm", true);
    }

    @Test
    void shouldForceUnitScopeFromPathOnInsert() throws Exception {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        MeasureUnitWebController controller = new MeasureUnitWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnit inserted = unit("crm", "weight", "kg");
        inserted.setId("unit-1");
        when(service.insert(any(MeasureUnit.class))).thenReturn("unit-1");
        when(service.select("unit-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.application/crm/measure-unit-categories/weight/units/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"other","categoryAlias":"other_category","code":"kg","title":"Kilogram"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("crm"))
                .andExpect(jsonPath("$.record.categoryAlias").value("weight"))
                .andExpect(jsonPath("$.record.code").value("kg"));

        ArgumentCaptor<MeasureUnit> captor = ArgumentCaptor.forClass(MeasureUnit.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("crm");
        assertThat(captor.getValue().getCategoryAlias()).isEqualTo("weight");
    }

    @Test
    void shouldExposeUnitOptionsWithinPathScope() throws Exception {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        MeasureUnitWebController controller = new MeasureUnitWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.listVisibleUnits("crm", "weight", true)).thenReturn(List.of(unit("platform", "weight", "kg")));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/platform.application/crm/measure-unit-categories/weight/units/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].code").value("kg"));

        verify(service).listVisibleUnits("crm", "weight", true);
    }

    @Test
    void shouldForceSharedCategoryApplicationAliasOnInsert() throws Exception {
        MeasureUnitCategoryService service = mock(MeasureUnitCategoryService.class);
        SharedMeasureUnitCategoryWebController controller = new SharedMeasureUnitCategoryWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnitCategory inserted = category("platform", "quantity");
        inserted.setId("cat-1");
        when(service.insert(any(MeasureUnitCategory.class))).thenReturn("cat-1");
        when(service.select("cat-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.measure_unit/categories/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"crm","alias":"quantity","title":"Quantity","dimension":"COUNT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("platform"))
                .andExpect(jsonPath("$.record.alias").value("quantity"));

        ArgumentCaptor<MeasureUnitCategory> captor = ArgumentCaptor.forClass(MeasureUnitCategory.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("platform");
    }

    @Test
    void shouldForceSharedUnitApplicationAliasOnInsert() throws Exception {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        SharedMeasureUnitWebController controller = new SharedMeasureUnitWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnit inserted = unit("platform", "quantity", "box");
        inserted.setId("unit-1");
        when(service.insert(any(MeasureUnit.class))).thenReturn("unit-1");
        when(service.select("unit-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.measure_unit/categories/quantity/units/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"crm","categoryAlias":"other","code":"box","title":"Box"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("platform"))
                .andExpect(jsonPath("$.record.categoryAlias").value("quantity"))
                .andExpect(jsonPath("$.record.code").value("box"));

        ArgumentCaptor<MeasureUnit> captor = ArgumentCaptor.forClass(MeasureUnit.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("platform");
        assertThat(captor.getValue().getCategoryAlias()).isEqualTo("quantity");
    }

    @Test
    void shouldConvertUnitsWithinPathScope() throws Exception {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        MeasureUnitWebController controller = new MeasureUnitWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(conversionService.convert("crm", "weight", new BigDecimal("2.5"), "kg", "g"))
                .thenReturn(new MeasureUnitConversion("crm", "weight", new BigDecimal("2.5"),
                        "kg", "g", new BigDecimal("2500"), new BigDecimal("2500")));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.application/crm/measure-unit-categories/weight/units/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":2.5,"fromUnitCode":"kg","toUnitCode":"g"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationAlias").value("crm"))
                .andExpect(jsonPath("$.categoryAlias").value("weight"))
                .andExpect(jsonPath("$.baseValue").value(2500));
    }

    private MeasureUnitCategory category(String applicationAlias, String alias) {
        MeasureUnitCategory category = new MeasureUnitCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setTitle(alias);
        category.setDimension(MeasureDimension.MASS);
        return category;
    }

    private MeasureUnit unit(String applicationAlias, String categoryAlias, String code) {
        MeasureUnit unit = new MeasureUnit();
        unit.setApplicationAlias(applicationAlias);
        unit.setCategoryAlias(categoryAlias);
        unit.setCode(code);
        unit.setTitle(code);
        unit.setEnabled(true);
        unit.setSortOrder(100);
        return unit;
    }
}
