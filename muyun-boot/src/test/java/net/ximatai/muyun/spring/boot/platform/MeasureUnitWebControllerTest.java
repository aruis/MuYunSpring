package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.platform.measure.MeasureDimension;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureUnitWebControllerTest {
    @Test
    void shouldDeclareApplicationScopedCategoryRoutesWithJaxRsAnnotations() throws Exception {
        assertThat(MeasureUnitCategoryWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.application/{applicationAlias}/measure-unit-categories");
        assertRoute(MeasureUnitCategoryWebController.class.getMethod("options", UriInfo.class, boolean.class),
                GET.class, "/options");
        assertRoute(method(NestedCrudWebSupport.class, "insert"), POST.class, "/insert");
    }

    @Test
    void shouldDeclareApplicationScopedUnitRoutesWithJaxRsAnnotations() throws Exception {
        assertThat(MeasureUnitWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units");
        assertRoute(MeasureUnitWebController.class.getMethod("options", UriInfo.class, boolean.class),
                GET.class, "/options");
        assertRoute(MeasureUnitWebController.class.getMethod("convert",
                UriInfo.class, MeasureUnitWebController.MeasureUnitConversionRequest.class),
                POST.class, "/convert");
        assertRoute(method(NestedCrudWebSupport.class, "insert"), POST.class, "/insert");
    }

    @Test
    void shouldForceCategoryApplicationAliasFromPathOnInsert() {
        MeasureUnitCategoryService service = mock(MeasureUnitCategoryService.class);
        TestMeasureUnitCategoryWebController controller = new TestMeasureUnitCategoryWebController(service);
        MeasureUnitCategory inserted = category("crm", "weight");
        inserted.setId("cat-1");
        when(service.insert(any(MeasureUnitCategory.class))).thenReturn("cat-1");
        when(service.select("cat-1")).thenReturn(inserted);

        WebRecordResponse<MeasureUnitCategory> response = controller.insert(
                request(Map.of("applicationAlias", "crm")),
                category("other", "weight"));

        assertThat(response.record().getApplicationAlias()).isEqualTo("crm");
        assertThat(response.record().getAlias()).isEqualTo("weight");
        ArgumentCaptor<MeasureUnitCategory> captor = ArgumentCaptor.forClass(MeasureUnitCategory.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("crm");
    }

    @Test
    void shouldExposeCategoryOptionsWithinPathApplication() {
        MeasureUnitCategoryService service = mock(MeasureUnitCategoryService.class);
        TestMeasureUnitCategoryWebController controller = new TestMeasureUnitCategoryWebController(service);
        when(service.listVisibleCategories("crm", true)).thenReturn(List.of(category("platform", "quantity")));

        WebListResponse<MeasureUnitCategory> response = controller.options(
                request(Map.of("applicationAlias", "crm")),
                true);

        assertThat(response.records()).singleElement()
                .satisfies(record -> {
                    assertThat(record.getApplicationAlias()).isEqualTo("platform");
                    assertThat(record.getAlias()).isEqualTo("quantity");
                });
        verify(service).listVisibleCategories("crm", true);
    }

    @Test
    void shouldForceUnitScopeFromPathOnInsert() {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        TestMeasureUnitWebController controller = new TestMeasureUnitWebController(service, conversionService);
        MeasureUnit inserted = unit("crm", "weight", "kg");
        inserted.setId("unit-1");
        when(service.insert(any(MeasureUnit.class))).thenReturn("unit-1");
        when(service.select("unit-1")).thenReturn(inserted);

        WebRecordResponse<MeasureUnit> response = controller.insert(
                request(Map.of("applicationAlias", "crm", "categoryAlias", "weight")),
                unit("other", "other_category", "kg"));

        assertThat(response.record().getApplicationAlias()).isEqualTo("crm");
        assertThat(response.record().getCategoryAlias()).isEqualTo("weight");
        assertThat(response.record().getCode()).isEqualTo("kg");
        ArgumentCaptor<MeasureUnit> captor = ArgumentCaptor.forClass(MeasureUnit.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("crm");
        assertThat(captor.getValue().getCategoryAlias()).isEqualTo("weight");
    }

    @Test
    void shouldExposeUnitOptionsWithinPathScope() {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        TestMeasureUnitWebController controller = new TestMeasureUnitWebController(service, conversionService);
        when(service.listVisibleUnits("crm", "weight", true)).thenReturn(List.of(unit("platform", "weight", "kg")));

        WebListResponse<MeasureUnit> response = controller.options(
                request(Map.of("applicationAlias", "crm", "categoryAlias", "weight")),
                true);

        assertThat(response.records()).singleElement()
                .satisfies(record -> assertThat(record.getCode()).isEqualTo("kg"));
        verify(service).listVisibleUnits("crm", "weight", true);
    }

    @Test
    void shouldConvertUnitsWithinPathScope() {
        MeasureUnitService service = mock(MeasureUnitService.class);
        MeasureUnitConversionService conversionService = mock(MeasureUnitConversionService.class);
        TestMeasureUnitWebController controller = new TestMeasureUnitWebController(service, conversionService);
        when(conversionService.convert("crm", "weight", new BigDecimal("2.5"), "kg", "g"))
                .thenReturn(new MeasureUnitConversion("crm", "weight", new BigDecimal("2.5"),
                        "kg", "g", new BigDecimal("2500"), new BigDecimal("2500")));

        MeasureUnitConversion response = controller.convert(
                request(Map.of("applicationAlias", "crm", "categoryAlias", "weight")),
                new MeasureUnitWebController.MeasureUnitConversionRequest(new BigDecimal("2.5"), "kg", "g"));

        assertThat(response.applicationAlias()).isEqualTo("crm");
        assertThat(response.categoryAlias()).isEqualTo("weight");
        assertThat(response.baseValue()).isEqualByComparingTo("2500");
    }

    private void assertRoute(Method method, Class<?> httpMethod, String path) {
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
    }

    private Method method(Class<?> type, String name) {
        return List.of(type.getMethods()).stream()
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private UriInfo request(Map<String, String> pathVariables) {
        UriInfo request = mock(UriInfo.class);
        MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        pathVariables.forEach(parameters::putSingle);
        when(request.getPathParameters()).thenReturn(parameters);
        return request;
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

    private static final class TestMeasureUnitCategoryWebController extends MeasureUnitCategoryWebController {
        private TestMeasureUnitCategoryWebController(MeasureUnitCategoryService service) {
            this.service = service;
        }
    }

    private static final class TestMeasureUnitWebController extends MeasureUnitWebController {
        private TestMeasureUnitWebController(MeasureUnitService service,
                                             MeasureUnitConversionService conversionService) {
            super(conversionService);
            this.service = service;
        }
    }
}
