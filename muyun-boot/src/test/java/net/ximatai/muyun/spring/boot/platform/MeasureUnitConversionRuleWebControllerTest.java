package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRule;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRuleService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionScopeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureUnitConversionRuleWebControllerTest {
    @Test
    void shouldDeclareApplicationAndSharedConversionRoutes() throws Exception {
        assertThat(MeasureUnitConversionRuleWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.application/{applicationAlias}/measure-unit-conversion-rules");
        assertEndpoint(MeasureUnitConversionRuleWebController.class.getMethod("convert",
                        HttpServletRequest.class,
                        MeasureUnitConversionRuleWebController.MeasureBusinessConversionRequest.class),
                "/convert");

        assertThat(SharedMeasureUnitConversionRuleWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.measure_unit/conversion-rules");
        assertEndpoint(SharedMeasureUnitConversionRuleWebController.class.getMethod("convert",
                        HttpServletRequest.class,
                        SharedMeasureUnitConversionRuleWebController.MeasureBusinessConversionRequest.class),
                "/convert");
    }

    @Test
    void shouldForceApplicationAliasFromPathOnInsert() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        MeasureUnitConversionRuleWebController controller =
                new MeasureUnitConversionRuleWebController(conversionService);
        setService(controller, service);
        MeasureUnitConversionRule inserted = rule("crm");
        inserted.setId("rule-1");
        when(service.insert(any(MeasureUnitConversionRule.class))).thenReturn("rule-1");
        when(service.select("rule-1")).thenReturn(inserted);

        WebRecordResponse<MeasureUnitConversionRule> response = controller.insert(
                applicationRequest("crm"),
                rule("other")
        );

        assertThat(response.record().getApplicationAlias()).isEqualTo("crm");
        ArgumentCaptor<MeasureUnitConversionRule> captor =
                ArgumentCaptor.forClass(MeasureUnitConversionRule.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("crm");
    }

    @Test
    void shouldPreviewBusinessConversionWithinPathApplication() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        MeasureUnitConversionRuleWebController controller =
                new MeasureUnitConversionRuleWebController(conversionService);
        setService(controller, service);
        when(conversionService.convert(any(MeasureUnitConversionContext.class), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle")))
                .thenReturn(new MeasureUnitBusinessConversion(
                        new MeasureUnitConversionContext("crm", "crm.order", "sku", "sku-1", null),
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle",
                        new BigDecimal("24"), List.of("rule-1")));

        MeasureUnitBusinessConversion response = controller.convert(applicationRequest("crm"),
                new MeasureUnitConversionRuleWebController.MeasureBusinessConversionRequest(
                        "crm.order", "sku", "sku-1", null,
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle"));

        assertThat(response.context().applicationAlias()).isEqualTo("crm");
        assertThat(response.convertedValue()).isEqualByComparingTo("24");
        ArgumentCaptor<MeasureUnitConversionContext> context =
                ArgumentCaptor.forClass(MeasureUnitConversionContext.class);
        verify(conversionService).convert(context.capture(), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle"));
        assertThat(context.getValue().applicationAlias()).isEqualTo("crm");
        assertThat(context.getValue().moduleAlias()).isEqualTo("crm.order");
        assertThat(context.getValue().contextObjectId()).isEqualTo("sku-1");
    }

    @Test
    void shouldForceSharedRuleApplicationAliasOnInsert() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        SharedMeasureUnitConversionRuleWebController controller =
                new SharedMeasureUnitConversionRuleWebController(conversionService);
        setService(controller, service);
        MeasureUnitConversionRule inserted = rule(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        inserted.setId("rule-1");
        when(service.insert(any(MeasureUnitConversionRule.class))).thenReturn("rule-1");
        when(service.select("rule-1")).thenReturn(inserted);

        WebRecordResponse<MeasureUnitConversionRule> response = controller.insert(null, rule("crm"));

        assertThat(response.record().getApplicationAlias())
                .isEqualTo(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        ArgumentCaptor<MeasureUnitConversionRule> captor =
                ArgumentCaptor.forClass(MeasureUnitConversionRule.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias())
                .isEqualTo(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Test
    void shouldPreviewSharedBusinessConversionForConsumerApplication() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        SharedMeasureUnitConversionRuleWebController controller =
                new SharedMeasureUnitConversionRuleWebController(conversionService);
        setService(controller, service);
        when(conversionService.convert(any(MeasureUnitConversionContext.class), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle")))
                .thenReturn(new MeasureUnitBusinessConversion(
                        new MeasureUnitConversionContext("crm", "crm.order", "sku", "sku-1", null),
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle",
                        new BigDecimal("24"), List.of("rule-1")));

        MeasureUnitBusinessConversion response = controller.convert(null,
                new SharedMeasureUnitConversionRuleWebController.MeasureBusinessConversionRequest(
                        "crm", "crm.order", "sku", "sku-1", null,
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle"));

        assertThat(response.context().applicationAlias()).isEqualTo("crm");
        assertThat(response.convertedValue()).isEqualByComparingTo("24");
        ArgumentCaptor<MeasureUnitConversionContext> context =
                ArgumentCaptor.forClass(MeasureUnitConversionContext.class);
        verify(conversionService).convert(context.capture(), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle"));
        assertThat(context.getValue().applicationAlias()).isEqualTo("crm");
        assertThat(context.getValue().moduleAlias()).isEqualTo("crm.order");
    }

    private MeasureUnitConversionRule rule(String applicationAlias) {
        MeasureUnitConversionRule rule = new MeasureUnitConversionRule();
        rule.setApplicationAlias(applicationAlias);
        rule.setScopeType(MeasureUnitConversionScopeType.GLOBAL);
        rule.setFromCategoryAlias("quantity");
        rule.setFromUnitCode("box");
        rule.setToCategoryAlias("quantity");
        rule.setToUnitCode("bottle");
        rule.setFactor(new BigDecimal("12"));
        rule.setTitle("box to bottle");
        return rule;
    }

    private HttpServletRequest applicationRequest(String applicationAlias) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(NestedCrudWebSupport.PATH_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("applicationAlias", applicationAlias));
        return request;
    }

    private void assertEndpoint(Method method, String path) {
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        assertThat(method.getAnnotation(ActionEndpoint.class).value()).isEqualTo(PlatformAction.QUERY);
    }

    private void setService(Object controller, Object service) throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(controller, service);
    }
}
