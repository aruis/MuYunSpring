package net.ximatai.muyun.spring.boot.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.boot.MuYunSpringJacksonConfiguration;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRule;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRuleService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionScopeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeasureUnitConversionRuleWebControllerTest {
    @Test
    void shouldForceApplicationAliasFromPathOnInsert() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        MeasureUnitConversionRuleWebController controller =
                new MeasureUnitConversionRuleWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnitConversionRule inserted = rule("crm");
        inserted.setId("rule-1");
        when(service.insert(any(MeasureUnitConversionRule.class))).thenReturn("rule-1");
        when(service.select("rule-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(codeTitleEnumConverter())
                .build();
        mvc.perform(post("/platform.application/crm/measure-unit-conversion-rules/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationAlias":"other",
                                  "scopeType":"global",
                                  "fromCategoryAlias":"quantity",
                                  "fromUnitCode":"box",
                                  "toCategoryAlias":"quantity",
                                  "toUnitCode":"bottle",
                                  "factor":12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("crm"));

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
        ReflectionTestUtils.setField(controller, "service", service);
        when(conversionService.convert(any(MeasureUnitConversionContext.class), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle")))
                .thenReturn(new MeasureUnitBusinessConversion(
                        new MeasureUnitConversionContext("crm", "crm.order", "sku", "sku-1", null),
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle",
                        new BigDecimal("24"), List.of("rule-1")));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(codeTitleEnumConverter())
                .build();
        mvc.perform(post("/platform.application/crm/measure-unit-conversion-rules/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleAlias":"crm.order",
                                  "contextObjectType":"sku",
                                  "contextObjectId":"sku-1",
                                  "value":2,
                                  "fromCategoryAlias":"quantity",
                                  "fromUnitCode":"box",
                                  "toCategoryAlias":"quantity",
                                  "toUnitCode":"bottle"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.applicationAlias").value("crm"))
                .andExpect(jsonPath("$.convertedValue").value(24));

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
        ReflectionTestUtils.setField(controller, "service", service);
        MeasureUnitConversionRule inserted = rule("platform");
        inserted.setId("rule-1");
        when(service.insert(any(MeasureUnitConversionRule.class))).thenReturn("rule-1");
        when(service.select("rule-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(codeTitleEnumConverter())
                .build();
        mvc.perform(post("/platform.measure_unit/conversion-rules/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationAlias":"crm",
                                  "scopeType":"global",
                                  "fromCategoryAlias":"quantity",
                                  "fromUnitCode":"box",
                                  "toCategoryAlias":"quantity",
                                  "toUnitCode":"bottle",
                                  "factor":12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.record.applicationAlias").value("platform"));

        ArgumentCaptor<MeasureUnitConversionRule> captor =
                ArgumentCaptor.forClass(MeasureUnitConversionRule.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("platform");
    }

    @Test
    void shouldPreviewSharedBusinessConversionForConsumerApplication() throws Exception {
        MeasureUnitConversionRuleService service = mock(MeasureUnitConversionRuleService.class);
        MeasureUnitBusinessConversionService conversionService = mock(MeasureUnitBusinessConversionService.class);
        SharedMeasureUnitConversionRuleWebController controller =
                new SharedMeasureUnitConversionRuleWebController(conversionService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(conversionService.convert(any(MeasureUnitConversionContext.class), eq(new BigDecimal("2")),
                eq("quantity"), eq("box"), eq("quantity"), eq("bottle")))
                .thenReturn(new MeasureUnitBusinessConversion(
                        new MeasureUnitConversionContext("crm", "crm.order", "sku", "sku-1", null),
                        new BigDecimal("2"), "quantity", "box", "quantity", "bottle",
                        new BigDecimal("24"), List.of("rule-1")));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(codeTitleEnumConverter())
                .build();
        mvc.perform(post("/platform.measure_unit/conversion-rules/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationAlias":"crm",
                                  "moduleAlias":"crm.order",
                                  "contextObjectType":"sku",
                                  "contextObjectId":"sku-1",
                                  "value":2,
                                  "fromCategoryAlias":"quantity",
                                  "fromUnitCode":"box",
                                  "toCategoryAlias":"quantity",
                                  "toUnitCode":"bottle"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.applicationAlias").value("crm"))
                .andExpect(jsonPath("$.convertedValue").value(24));

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

    private MappingJackson2HttpMessageConverter codeTitleEnumConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new MuYunSpringJacksonConfiguration().codeTitleEnumJacksonModule());
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
