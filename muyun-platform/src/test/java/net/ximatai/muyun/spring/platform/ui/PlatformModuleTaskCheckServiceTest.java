package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

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

        List<PlatformModuleTaskStatus> statuses = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(statuses).hasSize(1);
        assertThat(statuses.getFirst().status()).isEqualTo(PlatformTaskCompletionStatus.COMPLETE);
        assertThat(statuses.getFirst().matchedCount()).isEqualTo(1L);
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

        List<PlatformModuleTaskStatus> statuses = service.check("crm.customer", "customer-1", "ui-detail");

        assertThat(statuses.getFirst().status()).isEqualTo(PlatformTaskCompletionStatus.PENDING);
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(queryItemService).compile(eq("q-ready"), values.capture());
        assertThat(values.getValue()).containsEntry("recordId", "customer-1");
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
}
