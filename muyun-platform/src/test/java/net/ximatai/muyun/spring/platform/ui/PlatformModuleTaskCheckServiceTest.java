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
    void shouldRejectUnknownUiConfig() {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        when(snapshotService.snapshot("crm.customer")).thenReturn(snapshot("{}", List.of()));
        PlatformModuleTaskCheckService service = new PlatformModuleTaskCheckService(
                snapshotService, mock(PlatformQueryItemService.class), mock(DynamicRecordService.class));

        assertThatThrownBy(() -> service.check("crm.customer", "customer-1", "missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("UI config is not published");
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
}
