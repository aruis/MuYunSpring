package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelation;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformModuleTaskCheckServiceTest {

    @Test
    void shouldCheckAssociationViewTaskByExistingRelatedRecord() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("""
                {
                  "blocks": [
                    {"type":"taskPanel", "key":"contracts", "checkType":"ASSOCIATION_VIEW", "associationViewCode":"contracts"}
                  ]
                }
                """, List.of()));
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(recordService.associationViewPage(eq("crm.customer"), eq("customer"), eq("customer-1"),
                eq("contracts"), any(Criteria.class), any(PageRequest.class)))
                .thenReturn(PageResult.of(List.of(mock(DynamicRecord.class)), 1, new PageRequest(1, 1)));

        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService);

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.passed()).isTrue();
        List<PlatformModuleTaskStatus> statuses = result.tasks();
        assertThat(statuses).hasSize(1);
        assertThat(statuses.getFirst().status()).isEqualTo(PlatformTaskCompletionStatus.COMPLETE);
        assertThat(statuses.getFirst().passed()).isTrue();
        assertThat(statuses.getFirst().matchedCount()).isEqualTo(1L);
        assertThat(statuses.getFirst().expectedCount()).isEqualTo(1);
        assertThat(statuses.getFirst().checks()).hasSize(1);
    }

    @Test
    void shouldCheckQueryTemplateTaskWithCurrentRecordExternalValue() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        PlatformQueryTemplate template = queryTemplate("q-ready", "crm.customer");
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("""
                {
                  "blocks": [
                    {
                      "type":"taskPanel",
                      "key":"ready",
                      "checkType":"QUERY_TEMPLATE",
                      "queryTemplateId":"q-ready",
                      "externalRecordIdKey":"recordId"
                    }
                  ]
                }
                """, List.of(template)));
        Criteria compiled = Criteria.of().eq("id", "customer-1");
        when(queryItemService.compile(eq("q-ready"), any(Map.class))).thenReturn(compiled);
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(recordService.count("crm.customer", "customer", compiled)).thenReturn(0L);

        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService);

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.passed()).isFalse();
        List<PlatformModuleTaskStatus> statuses = result.tasks();
        assertThat(statuses.getFirst().status()).isEqualTo(PlatformTaskCompletionStatus.PENDING);
        assertThat(statuses.getFirst().passed()).isFalse();
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(queryItemService).compile(eq("q-ready"), values.capture());
        assertThat(values.getValue()).containsEntry("recordId", "customer-1");
    }

    @Test
    void shouldSummarizeMultipleChecksAndExpectedCount() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        RecordImpactRelationService impactRelationService = mock(RecordImpactRelationService.class);
        PlatformQueryTemplate template = queryTemplate("q-ready", "crm.customer");
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("""
                {
                  "blocks": [
                    {
                      "type":"taskPanel",
                      "key":"ready",
                      "checks": [
                        {
                          "checkType":"QUERY_TEMPLATE",
                          "queryTemplateId":"q-ready",
                          "externalRecordIdKey":"recordId",
                          "expectedCount":2
                        },
                        {
                          "checkType":"GENERATED_RELATION",
                          "targetModuleAlias":"crm.contract",
                          "generationRuleId":"rule-1"
                        }
                      ]
                    }
                  ]
                }
                """, List.of(template)));
        Criteria compiled = Criteria.of().eq("customerId", "customer-1");
        when(queryItemService.compile(eq("q-ready"), any(Map.class))).thenReturn(compiled);
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(recordService.mainEntityAlias("crm.contract")).thenReturn("contract");
        when(recordService.count("crm.customer", "customer", compiled)).thenReturn(2L);
        RecordImpactRelation relation = new RecordImpactRelation();
        relation.setTargetRecordId("contract-1");
        when(impactRelationService.listGeneratedTargets(eq("crm.customer"), eq("customer-1"),
                eq("crm.contract"), eq("rule-1"), any(PageRequest.class)))
                .thenReturn(List.of(relation));
        when(recordService.count(eq("crm.contract"), eq("contract"), any(Criteria.class))).thenReturn(1L);
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService, Optional.of(impactRelationService));

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.passed()).isTrue();
        PlatformModuleTaskStatus task = result.tasks().getFirst();
        assertThat(task.status()).isEqualTo(PlatformTaskCompletionStatus.COMPLETE);
        assertThat(task.matchedCount()).isEqualTo(3L);
        assertThat(task.expectedCount()).isEqualTo(3);
        assertThat(task.checks())
                .extracting(PlatformModuleTaskCheckDetail::checkType,
                        PlatformModuleTaskCheckDetail::passed,
                        PlatformModuleTaskCheckDetail::actualCount,
                        PlatformModuleTaskCheckDetail::expectedCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(PlatformTaskCheckType.QUERY_TEMPLATE, true, 2L, 2),
                        org.assertj.core.groups.Tuple.tuple(PlatformTaskCheckType.GENERATED_RELATION, true, 1L, 1)
                );
    }

    @Test
    void shouldContinueGeneratedRelationCheckUntilExpectedVisibleTargetsAreFound() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        RecordImpactRelationService impactRelationService = mock(RecordImpactRelationService.class);
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("""
                {
                  "blocks": [
                    {
                      "type":"taskPanel",
                      "key":"generated",
                      "checkType":"GENERATED_RELATION",
                      "targetModuleAlias":"crm.contract",
                      "expectedCount":2
                    }
                  ]
                }
                """, List.of()));
        when(recordService.mainEntityAlias("crm.contract")).thenReturn("contract");
        List<RecordImpactRelation> firstPage = java.util.stream.IntStream.range(0, 500)
                .mapToObj(index -> relation("deleted-" + index))
                .toList();
        when(impactRelationService.listGeneratedTargets(eq("crm.customer"), eq("customer-1"),
                eq("crm.contract"), eq(null), any(PageRequest.class)))
                .thenReturn(firstPage)
                .thenReturn(List.of(relation("contract-1"), relation("contract-2")));
        when(recordService.count(eq("crm.contract"), eq("contract"), any(Criteria.class)))
                .thenReturn(0L)
                .thenReturn(2L);
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService, Optional.of(impactRelationService));

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.passed()).isTrue();
        assertThat(result.tasks().getFirst().checks().getFirst().actualCount()).isEqualTo(2L);
    }

    @Test
    void shouldEvaluatePlatformTaskDefinitionWithGuides() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        PlatformQueryTemplate template = queryTemplate("q-ready", "crm.customer");
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("{}", List.of(template)));
        Criteria compiled = Criteria.of().eq("customerId", "customer-1");
        when(queryItemService.compile(eq("q-ready"), any(Map.class))).thenReturn(compiled);
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(recordService.count("crm.customer", "customer", compiled)).thenReturn(1L);
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();
        registry.register(new PlatformModuleTaskDefinition("crm.customer", "profile-ready", "资料齐备",
                PlatformModuleTaskType.BUSINESS_COMPLETION, PlatformModuleTaskOriginType.LOCAL_EDIT,
                "local-edit-basic", true, false, true, 10, "/crm.customer/view/{id}",
                List.of(new PlatformModuleTaskGuideDefinition("profile-ready",
                        PlatformModuleTaskGuideType.OPEN_FORM, "muyun.localEdit",
                        "/crm.customer/view/{id}", "crm.customer", "detail", "name", "补充资料")),
                List.of(new PlatformModuleTaskCheckDefinition("profile-ready", PlatformTaskCheckType.QUERY_TEMPLATE,
                        null, "q-ready", "recordId", null, null, 1, "/crm.customer/query"))));
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService, Optional.empty(), registry);

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.passed()).isTrue();
        PlatformModuleTaskStatus task = result.tasks().getFirst();
        assertThat(task.key()).isEqualTo("profile-ready");
        assertThat(task.guides()).hasSize(1);
        assertThat(task.guides().getFirst().guideType()).isEqualTo(PlatformModuleTaskGuideType.OPEN_FORM);
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(queryItemService).compile(eq("q-ready"), values.capture());
        assertThat(values.getValue()).containsEntry("recordId", "customer-1");
    }

    @Test
    void shouldPreferPlatformTaskDefinitionWhenUiBlockHasSameKey() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        PlatformQueryTemplate template = queryTemplate("q-ready", "crm.customer");
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("""
                {
                  "blocks": [
                    {
                      "type":"taskPanel",
                      "key":"ready",
                      "title":"页面任务",
                      "checkType":"ASSOCIATION_VIEW",
                      "associationViewCode":"contracts"
                    }
                  ]
                }
                """, List.of(template)));
        Criteria compiled = Criteria.of().eq("customerId", "customer-1");
        when(queryItemService.compile(eq("q-ready"), any(Map.class))).thenReturn(compiled);
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(recordService.count("crm.customer", "customer", compiled)).thenReturn(1L);
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();
        registry.register(new PlatformModuleTaskDefinition("crm.customer", "ready", "定义任务",
                PlatformModuleTaskType.BUSINESS_COMPLETION, PlatformModuleTaskOriginType.MANUAL,
                null, false, false, true, 1, null, List.of(),
                List.of(new PlatformModuleTaskCheckDefinition("ready", PlatformTaskCheckType.QUERY_TEMPLATE,
                        null, "q-ready", "recordId", null, null, 1, null))));
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, queryItemService, recordService, Optional.empty(), registry);

        PlatformModuleTaskCheckResult result = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(result.tasks()).singleElement()
                .satisfies(task -> {
                    assertThat(task.key()).isEqualTo("ready");
                    assertThat(task.title()).isEqualTo("定义任务");
                    assertThat(task.checkType()).isEqualTo(PlatformTaskCheckType.QUERY_TEMPLATE);
                });
    }

    @Test
    void shouldRejectUnknownUiConfig() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("{}", List.of()));
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, mock(PlatformQueryItemService.class), mock(DynamicRecordService.class));

        assertThatThrownBy(() -> service.check("crm.customer", "customer-1", "missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("UI config is not published");
    }

    @Test
    void shouldRejectCrossModuleDefinitionsWhenReplacingRegistryModule() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();

        assertThatThrownBy(() -> registry.replace("crm.customer", List.of(new PlatformModuleTaskDefinition(
                "crm.contract", "ready", "合同任务", PlatformModuleTaskType.BUSINESS_COMPLETION,
                PlatformModuleTaskOriginType.MANUAL, null, false, false, true, 1, null, List.of(), List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Module task definition does not belong");
    }

    @Test
    void shouldSyncManagedSourceWithoutOverwritingManualTasks() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();
        registry.register(task("crm.customer", "manual-ready", PlatformModuleTaskOriginType.MANUAL,
                null, false, "手工任务"));

        registry.replaceManagedSource("crm.customer", PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "profile-ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", true, "资料齐备")));

        assertThat(registry.listEnabled("crm.customer"))
                .extracting(PlatformModuleTaskDefinition::taskCode)
                .containsExactly("manual-ready", "profile-ready");
    }

    @Test
    void shouldReplaceOldManagedTasksFromSameSource() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();
        registry.replaceManagedSource("crm.customer", PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "old-ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", true, "旧任务")));

        registry.replaceManagedSource("crm.customer", PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "new-ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", true, "新任务")));

        assertThat(registry.listEnabled("crm.customer"))
                .extracting(PlatformModuleTaskDefinition::taskCode)
                .containsExactly("new-ready");
    }

    @Test
    void shouldRejectInvalidManagedSourceSyncDefinitions() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();

        assertThatThrownBy(() -> registry.replaceManagedSource("crm.customer",
                PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.contract", "ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", true, "跨模块"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        assertThatThrownBy(() -> registry.replaceManagedSource("crm.customer",
                PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", false, "非托管"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only accepts managed");
        assertThatThrownBy(() -> registry.replaceManagedSource("crm.customer",
                PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "ready", PlatformModuleTaskOriginType.GENERATION_RULE,
                        "rule-1", true, "错误来源"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matching origin");
    }

    @Test
    void shouldRejectManagedSourceSyncWhenTaskCodeIsOwnedByManualTask() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();
        registry.register(task("crm.customer", "ready", PlatformModuleTaskOriginType.MANUAL,
                null, false, "手工任务"));

        assertThatThrownBy(() -> registry.replaceManagedSource("crm.customer",
                PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(task("crm.customer", "ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                        "local-edit-basic", true, "托管任务"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already owned by another source");
    }

    @Test
    void shouldRejectDuplicateTaskCodeWithinManagedSourceSync() {
        PlatformModuleTaskDefinitionRegistry registry = new PlatformModuleTaskDefinitionRegistry();

        assertThatThrownBy(() -> registry.replaceManagedSource("crm.customer",
                PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic",
                List.of(
                        task("crm.customer", "ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                                "local-edit-basic", true, "任务一"),
                        task("crm.customer", "ready", PlatformModuleTaskOriginType.LOCAL_EDIT,
                                "local-edit-basic", true, "任务二")
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate taskCode");
    }

    private PlatformPageConfigSnapshot snapshot(String layoutJson, List<PlatformQueryTemplate> templates) {
        PlatformUiConfig config = new PlatformUiConfig();
        config.setId("ui-detail");
        config.setClientType(PlatformUiClientType.WEB);
        config.setLayoutJson(layoutJson);
        return new PlatformPageConfigSnapshot("crm.customer",
                List.of(), List.of(config), List.of(), templates, List.of());
    }

    private PlatformQueryTemplate queryTemplate(String id, String moduleAlias) {
        PlatformQueryTemplate template = new PlatformQueryTemplate();
        template.setId(id);
        template.setModuleAlias(moduleAlias);
        template.setEnabled(true);
        template.setPublished(true);
        return template;
    }

    private RecordImpactRelation relation(String targetRecordId) {
        RecordImpactRelation relation = new RecordImpactRelation();
        relation.setTargetRecordId(targetRecordId);
        return relation;
    }

    private PlatformModuleTaskDefinition task(String moduleAlias,
                                              String taskCode,
                                              PlatformModuleTaskOriginType originType,
                                              String originId,
                                              boolean managed,
                                              String title) {
        return new PlatformModuleTaskDefinition(moduleAlias, taskCode, title,
                PlatformModuleTaskType.BUSINESS_COMPLETION, originType, originId, managed,
                false, true, 1, null, List.of(), List.of());
    }
}
