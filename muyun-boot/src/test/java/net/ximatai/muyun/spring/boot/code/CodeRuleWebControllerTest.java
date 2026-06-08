package net.ximatai.muyun.spring.boot.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntryService;
import net.ximatai.muyun.spring.platform.code.CodeLedgerStatus;
import net.ximatai.muyun.spring.platform.code.CodePreviewResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewSegmentResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import net.ximatai.muyun.spring.platform.code.CodeRecycleStatus;
import net.ximatai.muyun.spring.platform.code.CodeRule;
import net.ximatai.muyun.spring.platform.code.CodeRuleSegment;
import net.ximatai.muyun.spring.platform.code.CodeRuleService;
import net.ximatai.muyun.spring.platform.code.CodeSegmentType;
import net.ximatai.muyun.spring.platform.code.CodeSequencePolicy;
import net.ximatai.muyun.spring.platform.code.CodeSequenceState;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateService;
import net.ximatai.muyun.spring.platform.code.PreviewCodeRuleCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeRuleWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CodeRuleService ruleService;
    private CodePreviewService previewService;
    private CodeSequenceStateService stateService;
    private CodeLedgerEntryService ledgerService;
    private CodeRecycleEntryService recycleService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ruleService = mock(CodeRuleService.class);
        previewService = mock(CodePreviewService.class);
        stateService = mock(CodeSequenceStateService.class);
        ledgerService = mock(CodeLedgerEntryService.class);
        recycleService = mock(CodeRecycleEntryService.class);

        CodeRuleWebController ruleController = new CodeRuleWebController(previewService);
        CodeSequenceStateWebController stateController = new CodeSequenceStateWebController();
        CodeLedgerEntryWebController ledgerController = new CodeLedgerEntryWebController();
        CodeRecycleEntryWebController recycleController = new CodeRecycleEntryWebController();
        ReflectionTestUtils.setField(ruleController, "service", ruleService);
        ReflectionTestUtils.setField(stateController, "service", stateService);
        ReflectionTestUtils.setField(ledgerController, "service", ledgerService);
        ReflectionTestUtils.setField(recycleController, "service", recycleService);

        mvc = MockMvcBuilders
                .standaloneSetup(ruleController, stateController, ledgerController, recycleController)
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant-a"))))
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndViewCodeRuleTreeThroughAliasPath() throws Exception {
        CodeRule saved = rule("rule-1");
        CodeRuleSegment segment = constantSegment("seg-1", "SO-");
        saved.setSegments(List.of(segment));
        CodeSequencePolicy policy = new CodeSequencePolicy();
        policy.setStartValue(100L);
        saved.setSequencePolicy(policy);
        when(ruleService.saveRuleTree(any(CodeRule.class))).thenReturn(saved);
        when(ruleService.viewRuleTree("rule-1")).thenReturn(saved);

        mvc.perform(post("/platform.code_rule/saveTree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rule-1"))
                .andExpect(jsonPath("$.segments[0].id").value("seg-1"))
                .andExpect(jsonPath("$.segments[0].fixedValue").value("SO-"))
                .andExpect(jsonPath("$.sequencePolicy.startValue").value(100));

        mvc.perform(get("/platform.code_rule/viewTree/rule-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rule-1"))
                .andExpect(jsonPath("$.segments[0].segmentType").value("CONSTANT"));

        verify(ruleService).saveRuleTree(any(CodeRule.class));
        verify(ruleService).viewRuleTree("rule-1");
    }

    @Test
    void shouldPreviewSavedRuleWithoutConsumingSequence() throws Exception {
        CodeRule saved = rule("rule-1");
        saved.setSegments(List.of(constantSegment("seg-1", "SO-")));
        when(ruleService.viewRuleTree("rule-1")).thenReturn(saved);
        when(previewService.previewDraft(any(PreviewCodeRuleCommand.class))).thenReturn(
                new CodePreviewResult("SO-001", List.of(
                        new CodePreviewSegmentResult("seg-1", CodeSegmentType.CONSTANT, "SO-", false)
                )));

        mvc.perform(post("/platform.code_rule/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleId":"rule-1",
                                  "context":{"orderType":"repair"},
                                  "sequenceValue":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("SO-001"))
                .andExpect(jsonPath("$.segments[0].segmentId").value("seg-1"));

        ArgumentCaptor<PreviewCodeRuleCommand> captor = ArgumentCaptor.forClass(PreviewCodeRuleCommand.class);
        verify(previewService).previewDraft(captor.capture());
        assertThat(captor.getValue().rule()).isSameAs(saved);
        assertThat(captor.getValue().sequenceValue()).isEqualTo(1L);
        assertThat(captor.getValue().context()).containsEntry("orderType", "repair");
        verify(ruleService).viewRuleTree("rule-1");
        verifyNoMoreInteractions(previewService);
    }

    @Test
    void shouldPreviewDraftRuleWithoutSavingRuleTree() throws Exception {
        when(previewService.previewDraft(any(PreviewCodeRuleCommand.class))).thenReturn(
                new CodePreviewResult("DRAFT", List.of()));

        mvc.perform(post("/platform/code/rule/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rule":{
                                    "moduleAlias":"crm.order",
                                    "entityAlias":"main",
                                    "fieldName":"orderNo",
                                    "segments":[{"segmentType":"CONSTANT","fixedValue":"DRAFT"}]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("DRAFT"));

        ArgumentCaptor<PreviewCodeRuleCommand> captor = ArgumentCaptor.forClass(PreviewCodeRuleCommand.class);
        verify(previewService).previewDraft(captor.capture());
        assertThat(captor.getValue().rule().getId()).isNull();
        assertThat(captor.getValue().rule().getSegments()).singleElement()
                .extracting(CodeRuleSegment::getFixedValue)
                .isEqualTo("DRAFT");
        verifyNoMoreInteractions(ruleService);
    }

    @Test
    void shouldExposeLifecycleStateLedgerAndRecycleAsReadOnlyQueryAndView() throws Exception {
        CodeSequenceState state = new CodeSequenceState();
        state.setId("state-1");
        state.setRuleId("rule-1");
        state.setBasisKey("basis");
        state.setPeriodKey("202606");
        state.setCurrentValue(12L);
        when(stateService.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(state), 1, PageRequest.of(1, 20)));
        when(stateService.select("state-1")).thenReturn(state);

        CodeLedgerEntry ledger = new CodeLedgerEntry();
        ledger.setId("ledger-1");
        ledger.setRuleId("rule-1");
        ledger.setModuleAlias("crm.order");
        ledger.setEntityAlias("main");
        ledger.setFieldName("orderNo");
        ledger.setCodeValue("SO-001");
        ledger.setStatus(CodeLedgerStatus.ACTIVE);
        when(ledgerService.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(ledger), 1, PageRequest.of(1, 20)));
        when(ledgerService.select("ledger-1")).thenReturn(ledger);

        CodeRecycleEntry recycle = new CodeRecycleEntry();
        recycle.setId("recycle-1");
        recycle.setRuleId("rule-1");
        recycle.setRecycledValue("SO-000");
        recycle.setStatus(CodeRecycleStatus.AVAILABLE);
        when(recycleService.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(recycle), 1, PageRequest.of(1, 20)));
        when(recycleService.select("recycle-1")).thenReturn(recycle);

        mvc.perform(post("/platform.code_sequence_state/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"ruleId","operator":"EQ","values":["rule-1"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("state-1"))
                .andExpect(jsonPath("$.records[0].currentValue").value(12));
        mvc.perform(get("/platform.code_sequence_state/view/state-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value("rule-1"));

        mvc.perform(post("/platform.code_ledger_entry/query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].codeValue").value("SO-001"));
        mvc.perform(get("/platform.code_ledger_entry/view/ledger-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/platform.code_recycle_entry/query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].recycledValue").value("SO-000"));
        mvc.perform(get("/platform.code_recycle_entry/view/recycle-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mvc.perform(post("/platform.code_sequence_state/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/platform.code_ledger_entry/update/ledger-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/platform.code_recycle_entry/delete/recycle-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotExposeRawCrudEndpointsForCodeRuleTree() throws Exception {
        mvc.perform(post("/platform.code_rule/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule(null))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/platform.code_rule/update/rule-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule("rule-1"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/platform.code_rule/delete/rule-1"))
                .andExpect(status().isNotFound());
    }

    private CodeRule rule(String id) {
        CodeRule rule = new CodeRule();
        rule.setId(id);
        rule.setModuleAlias("crm.order");
        rule.setEntityAlias("main");
        rule.setFieldName("orderNo");
        return rule;
    }

    private CodeRuleSegment constantSegment(String id, String value) {
        CodeRuleSegment segment = new CodeRuleSegment();
        segment.setId(id);
        segment.setSegmentType(CodeSegmentType.CONSTANT);
        segment.setFixedValue(value);
        return segment;
    }
}
