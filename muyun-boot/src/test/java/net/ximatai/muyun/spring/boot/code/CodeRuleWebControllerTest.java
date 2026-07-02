package net.ximatai.muyun.spring.boot.code;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerInspection;
import net.ximatai.muyun.spring.platform.code.CodeLedgerInconsistencyReason;
import net.ximatai.muyun.spring.platform.code.CodeLedgerStatus;
import net.ximatai.muyun.spring.platform.code.CodeOpsActionService;
import net.ximatai.muyun.spring.platform.code.CodeOpsQueryService;
import net.ximatai.muyun.spring.platform.code.CodePreviewResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewSegmentResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.code.CodeRule;
import net.ximatai.muyun.spring.platform.code.CodeRuleOpsSnapshot;
import net.ximatai.muyun.spring.platform.code.CodeRuleSegment;
import net.ximatai.muyun.spring.platform.code.CodeRuleService;
import net.ximatai.muyun.spring.platform.code.CodeSegmentType;
import net.ximatai.muyun.spring.platform.code.CodeSequenceBaselineResult;
import net.ximatai.muyun.spring.platform.code.CodeSequencePolicy;
import net.ximatai.muyun.spring.platform.code.CodeSequenceState;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateLocation;
import net.ximatai.muyun.spring.platform.code.PreviewCodeRuleCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CodeRuleWebControllerTest {
    private final CodeRuleService ruleService = mock(CodeRuleService.class);
    private final CodePreviewService previewService = mock(CodePreviewService.class);
    private final CodeOpsQueryService opsQueryService = mock(CodeOpsQueryService.class);
    private final CodeOpsActionService opsActionService = mock(CodeOpsActionService.class);
    private final CodeRuleWebController controller =
            new CodeRuleWebController(previewService, opsQueryService, opsActionService);

    @Test
    void shouldDeclareCodeRuleRoutesAndActionMetadata() throws Exception {
        assertThat(CodeRuleWebController.class.getAnnotation(Path.class).value()).isEqualTo("/platform.code_rule");
        PlatformStaticModule module = CodeRuleWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.alias()).isEqualTo(CodeRuleService.MODULE_ALIAS);
        PlatformMenu menu = CodeRuleWebController.class.getAnnotation(PlatformMenu.class);
        assertThat(menu.parent()).isEqualTo(PlatformMenuGroups.OPS);

        assertActionRoute("viewTree", new Class<?>[]{String.class}, GET.class,
                "/viewTree/{id}", "viewTree", PlatformActionLevel.RECORD, true, "id");
        assertActionRoute("saveTree", new Class<?>[]{CodeRule.class}, POST.class,
                "/saveTree", "saveTree", PlatformActionLevel.ANY, false, "id");
        assertActionRoute("preview", new Class<?>[]{CodeRuleWebController.PreviewRequest.class}, POST.class,
                "/preview", "preview", PlatformActionLevel.ANY, false, "id");
        assertActionRoute("viewOpsSnapshot",
                new Class<?>[]{String.class, CodeRuleWebController.OpsSnapshotRequest.class}, POST.class,
                "/ops/view/{id}", "opsQuery", PlatformActionLevel.RECORD, true, "id");
        assertActionRoute("setSequenceBaseline",
                new Class<?>[]{CodeRuleWebController.SequenceBaselineRequest.class}, POST.class,
                "/ops/sequenceState/baseline", "opsManage", PlatformActionLevel.LIST, false, "id");
        assertActionRoute("inspectLedgerEntry", new Class<?>[]{String.class}, POST.class,
                "/ops/ledgerEntry/{id}/inspect", "opsQuery", PlatformActionLevel.RECORD, true, "id");
    }

    @Test
    void shouldSaveAndViewCodeRuleTree() throws Exception {
        setService(controller, ruleService);
        CodeRule saved = rule("rule-1");
        CodeRuleSegment segment = constantSegment("seg-1", "SO-");
        saved.setSegments(List.of(segment));
        CodeSequencePolicy policy = new CodeSequencePolicy();
        policy.setStartValue(100L);
        saved.setSequencePolicy(policy);
        when(ruleService.saveRuleTree(any(CodeRule.class))).thenReturn(saved);
        when(ruleService.viewRuleTree("rule-1")).thenReturn(saved);

        CodeRule saveResponse = inTenant(() -> controller.saveTree(rule(null)));
        CodeRule viewResponse = inTenant(() -> controller.viewTree("rule-1"));

        assertThat(saveResponse.getId()).isEqualTo("rule-1");
        assertThat(saveResponse.getSegments()).singleElement()
                .extracting(CodeRuleSegment::getFixedValue)
                .isEqualTo("SO-");
        assertThat(saveResponse.getSequencePolicy().getStartValue()).isEqualTo(100L);
        assertThat(viewResponse).isSameAs(saved);
        verify(ruleService).saveRuleTree(any(CodeRule.class));
        verify(ruleService).viewRuleTree("rule-1");
    }

    @Test
    void shouldPreviewSavedRuleWithoutConsumingSequence() throws Exception {
        setService(controller, ruleService);
        CodeRule saved = rule("rule-1");
        saved.setSegments(List.of(constantSegment("seg-1", "SO-")));
        when(ruleService.viewRuleTree("rule-1")).thenReturn(saved);
        when(previewService.previewDraft(any(PreviewCodeRuleCommand.class))).thenReturn(
                new CodePreviewResult("SO-001", List.of(
                        new CodePreviewSegmentResult("seg-1", CodeSegmentType.CONSTANT, "SO-", false)
                )));

        CodePreviewResult response = inTenant(() -> controller.preview(new CodeRuleWebController.PreviewRequest(
                "rule-1", null, Map.of("orderType", "repair"), null, null, 1L)));

        assertThat(response.value()).isEqualTo("SO-001");
        assertThat(response.segments()).singleElement()
                .extracting(CodePreviewSegmentResult::segmentId)
                .isEqualTo("seg-1");
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
        setService(controller, ruleService);
        CodeRule draft = rule(null);
        draft.setSegments(List.of(constantSegment(null, "DRAFT")));
        when(previewService.previewDraft(any(PreviewCodeRuleCommand.class))).thenReturn(
                new CodePreviewResult("DRAFT", List.of()));

        CodePreviewResult response = inTenant(() -> controller.preview(new CodeRuleWebController.PreviewRequest(
                null, draft, Map.of(), null, null, null)));

        assertThat(response.value()).isEqualTo("DRAFT");
        ArgumentCaptor<PreviewCodeRuleCommand> captor = ArgumentCaptor.forClass(PreviewCodeRuleCommand.class);
        verify(previewService).previewDraft(captor.capture());
        assertThat(captor.getValue().rule().getId()).isNull();
        assertThat(captor.getValue().rule().getSegments()).singleElement()
                .extracting(CodeRuleSegment::getFixedValue)
                .isEqualTo("DRAFT");
        verifyNoMoreInteractions(ruleService);
    }

    @Test
    void shouldExposeCodeOpsEndpointsThroughRuleModule() throws Exception {
        setService(controller, ruleService);
        CodeRule rule = rule("rule-1");
        CodeRuleOpsSnapshot snapshot = new CodeRuleOpsSnapshot(rule, List.of(), List.of(), List.of(), List.of());
        when(opsQueryService.viewRuleSnapshot("rule-1", 5)).thenReturn(snapshot);
        when(opsQueryService.queryBusinessObjectSnapshots("crm.order", "main", 3)).thenReturn(List.of(snapshot));
        when(opsQueryService.locateSequenceState("rule-1", "basis", "202606")).thenReturn(
                new CodeSequenceStateLocation("rule-1", "basis", "202606", false, null, 1L, "missing"));
        CodeSequenceState state = new CodeSequenceState();
        state.setId("state-1");
        state.setRuleId("rule-1");
        state.setBasisKey("basis");
        state.setPeriodKey("202606");
        state.setCurrentValue(100L);
        when(opsActionService.setSequenceBaseline("rule-1", "basis", "202606", 100L, "import")).thenReturn(
                new CodeSequenceBaselineResult(state, null, 100L, 101L, "updated"));
        CodeLedgerEntry ledger = new CodeLedgerEntry();
        ledger.setId("ledger-1");
        ledger.setRuleId("rule-1");
        ledger.setCodeValue("SO-001");
        ledger.setStatus(CodeLedgerStatus.ACTIVE);
        when(opsActionService.inspectLedgerEntry("ledger-1")).thenReturn(new CodeLedgerInspection(
                ledger,
                "SO-002",
                false,
                CodeLedgerInconsistencyReason.VALUE_CHANGED,
                true,
                "changed"
        ));

        assertThat(inTenant(() -> controller.viewOpsSnapshot("rule-1",
                new CodeRuleWebController.OpsSnapshotRequest(null, null, 5))).rule()).isSameAs(rule);
        assertThat(inTenant(() -> controller.queryOpsSnapshots(new CodeRuleWebController.OpsSnapshotRequest(
                "crm.order", "main", 3)))).containsExactly(snapshot);
        assertThat(inTenant(() -> controller.locateSequenceState(new CodeRuleWebController.SequenceBucketRequest(
                "rule-1", "basis", "202606"))).nextValue()).isEqualTo(1L);
        assertThat(inTenant(() -> controller.setSequenceBaseline(new CodeRuleWebController.SequenceBaselineRequest(
                "rule-1", "basis", "202606", 100L, "import"))).nextValue()).isEqualTo(101L);
        assertThat(inTenant(() -> controller.inspectLedgerEntry("ledger-1")).consistent()).isFalse();

        verify(opsQueryService).viewRuleSnapshot("rule-1", 5);
        verify(opsQueryService).queryBusinessObjectSnapshots("crm.order", "main", 3);
        verify(opsQueryService).locateSequenceState("rule-1", "basis", "202606");
        verify(opsActionService).setSequenceBaseline("rule-1", "basis", "202606", 100L, "import");
        verify(opsActionService).inspectLedgerEntry("ledger-1");
    }

    @Test
    void shouldRequireConfiguredOpsServices() throws Exception {
        setService(new CodeRuleWebController(previewService), ruleService);
        CodeRuleWebController noOpsController = new CodeRuleWebController(previewService);
        setService(noOpsController, ruleService);

        assertThatThrownBy(() -> inTenant(() -> noOpsController.viewOpsSnapshot("rule-1", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Code ops query service is not configured");
        assertThatThrownBy(() -> inTenant(() -> noOpsController.setSequenceBaseline(
                new CodeRuleWebController.SequenceBaselineRequest("rule-1", "basis", "period", 1L, "repair"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Code ops action service is not configured");
    }

    private void assertActionRoute(String methodName,
                                   Class<?>[] parameterTypes,
                                   Class<?> httpMethod,
                                   String path,
                                   String actionCode,
                                   PlatformActionLevel level,
                                   boolean dataAuth,
                                   String recordIdPathVariable) throws Exception {
        Method method = CodeRuleWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(level);
        assertThat(endpoint.dataAuth()).isEqualTo(dataAuth);
        assertThat(endpoint.recordIdPathVariable()).isEqualTo(recordIdPathVariable);
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

    private <T> T inTenant(Supplier<T> supplier) {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            return supplier.get();
        }
    }

    private void setService(CodeRuleWebController target, CodeRuleService service) throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(target, service);
    }
}
