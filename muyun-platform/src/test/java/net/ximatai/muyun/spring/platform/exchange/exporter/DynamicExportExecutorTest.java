package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordActionGateway;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicExportExecutorTest {
    private static final String MODULE = "sales.order";

    private final DynamicRecordService recordService = mock(DynamicRecordService.class);
    private final DynamicRecordActionGateway records = mock(DynamicRecordActionGateway.class);
    private final DynamicExportExecutor executor = new DynamicExportExecutor(
            recordService, new DynamicExchangeTemplatePlanBuilder());

    @Test
    void shouldExportMainRecordsThroughExportActionGateway() {
        EntityDefinition entity = new EntityDefinition("order", "sales_order", "Order", List.of(
                FieldDefinition.string("orderNo", "Order No").column("order_no"),
                FieldDefinition.string("status", "Status")
        ));
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                MODULE,
                "Order",
                List.of(entity)
        ));
        Criteria criteria = Criteria.of().eq("status", "active");
        PageRequest pageRequest = PageRequest.of(1, 20);
        Sort sort = Sort.asc("orderNo");
        DynamicRecord first = new DynamicRecord(entity);
        first.setId("order-1");
        first.setValue("orderNo", "SO-001");
        first.setValue("status", "active");
        when(recordService.recordsForAction(MODULE, PlatformAction.EXPORT, "dynamic-export")).thenReturn(records);
        when(records.list("order", criteria, pageRequest, sort)).thenReturn(List.of(first));

        ExcelWorkbookPlan plan = executor.export(new DynamicExportCommand(
                descriptor, criteria, pageRequest, List.of(sort)));

        verify(recordService).recordsForAction(MODULE, PlatformAction.EXPORT, "dynamic-export");
        ExcelSheetPlan main = plan.sheets().getFirst();
        assertThat(main.rows()).containsExactly(List.of("order-1", "SO-001", "active"));
    }
}
