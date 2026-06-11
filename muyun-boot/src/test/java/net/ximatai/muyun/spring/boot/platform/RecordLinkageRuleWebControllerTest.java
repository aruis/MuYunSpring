package net.ximatai.muyun.spring.boot.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRule;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRuleService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordLinkageRuleWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBindGenerationRuleSourceModuleFromPathWhenSavingTree() throws Exception {
        RecordGenerationRuleService service = mock(RecordGenerationRuleService.class);
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        RecordGenerationRule saved = generationRule("rule-1", "sales.contract");
        saved.setTargetModuleAlias("sales.invoice");
        when(service.saveRuleTree(any(RecordGenerationRule.class))).thenReturn(saved);

        mvc(controller).perform(post("/platform.module/sales.contract/generation-rules/saveTree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceModuleAlias":"other.module","targetModuleAlias":"sales.invoice","actionCode":"generateInvoice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceModuleAlias").value("sales.contract"));

        ArgumentCaptor<RecordGenerationRule> captor = ArgumentCaptor.forClass(RecordGenerationRule.class);
        verify(service).saveRuleTree(captor.capture());
        assertThat(captor.getValue().getSourceModuleAlias()).isEqualTo("sales.contract");
    }

    @Test
    void shouldRejectGenerationRuleTreeUpdateOutsidePathModule() throws Exception {
        RecordGenerationRuleService service = mock(RecordGenerationRuleService.class);
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("rule-1")).thenReturn(generationRule("rule-1", "sales.contract"));

        RecordGenerationRule incoming = generationRule("rule-1", "sales.invoice");

        assertThatThrownBy(() -> controller.saveTree(requestVars("sales.invoice"), incoming))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rule does not belong to module");
    }

    @Test
    void shouldQueryAndViewGenerationRulesWithinPathModule() throws Exception {
        RecordGenerationRuleService service = mock(RecordGenerationRuleService.class);
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        RecordGenerationRule rule = generationRule("rule-1", "sales.contract");
        rule.setActionCode("generateInvoice");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(rule), 1, PageRequest.of(1, 20)));
        when(service.select("rule-1")).thenReturn(rule);
        when(service.viewRuleTree("rule-1")).thenReturn(rule);

        MockMvc mvc = mvc(controller);
        mvc.perform(post("/platform.module/sales.contract/generation-rules/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"actionCode","values":["generateInvoice"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].sourceModuleAlias").value("sales.contract"));
        mvc.perform(get("/platform.module/sales.contract/generation-rules/viewTree/rule-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rule-1"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort[].class));
        assertClause(criteria.getValue(), "sourceModuleAlias", "sales.contract");
        assertClause(criteria.getValue(), "actionCode", "generateInvoice");
        verify(service).viewRuleTree("rule-1");
    }

    @Test
    void shouldBindWriteBackRuleTriggerModuleFromPathWhenSavingTree() throws Exception {
        RecordWriteBackRuleService service = mock(RecordWriteBackRuleService.class);
        RecordWriteBackRuleWebController controller = new RecordWriteBackRuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        RecordWriteBackRule saved = writeBackRule("rule-1", "sales.invoice");
        saved.setTargetModuleAlias("sales.contract");
        when(service.saveRuleTree(any(RecordWriteBackRule.class))).thenReturn(saved);

        mvc(controller).perform(post("/platform.module/sales.invoice/write-back-rules/saveTree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeBackRule(null, "other.module"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggerModuleAlias").value("sales.invoice"));

        ArgumentCaptor<RecordWriteBackRule> captor = ArgumentCaptor.forClass(RecordWriteBackRule.class);
        verify(service).saveRuleTree(captor.capture());
        assertThat(captor.getValue().getTriggerModuleAlias()).isEqualTo("sales.invoice");
    }

    @Test
    void shouldRejectWriteBackRuleTreeUpdateOutsidePathModule() throws Exception {
        RecordWriteBackRuleService service = mock(RecordWriteBackRuleService.class);
        RecordWriteBackRuleWebController controller = new RecordWriteBackRuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("rule-1")).thenReturn(writeBackRule("rule-1", "sales.invoice"));

        assertThatThrownBy(() -> controller.saveTree(
                requestVars("sales.contract"), writeBackRule("rule-1", "sales.contract")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rule does not belong to module");
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    private RecordGenerationRule generationRule(String id, String sourceModuleAlias) {
        RecordGenerationRule rule = new RecordGenerationRule();
        rule.setId(id);
        rule.setSourceModuleAlias(sourceModuleAlias);
        rule.setTitle("Generate invoice");
        return rule;
    }

    private RecordWriteBackRule writeBackRule(String id, String triggerModuleAlias) {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setId(id);
        rule.setTriggerModuleAlias(triggerModuleAlias);
        rule.setTargetModuleAlias("sales.contract");
        rule.setTitle("Write back contract");
        return rule;
    }

    private void assertClause(Criteria criteria, String field, Object value) {
        CriteriaClause clause = criteria.getClauses().stream()
                .filter(item -> field.equals(item.getField()))
                .findFirst()
                .orElseThrow();
        assertThat(clause.getOperator()).isEqualTo(CriteriaOperator.EQ);
        assertThat(clause.getValues()).containsExactly(value);
    }

    private MockHttpServletRequest requestVars(String moduleAlias) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("moduleAlias", moduleAlias));
        return request;
    }
}
