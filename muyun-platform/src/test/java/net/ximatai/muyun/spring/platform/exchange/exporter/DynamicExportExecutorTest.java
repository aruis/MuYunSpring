package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordActionGateway;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void shouldExportFirstLevelChildRowsWithParentRelateId() {
        EntityDefinition order = new EntityDefinition("order", "sales_order", "Order", List.of(
                FieldDefinition.string("orderNo", "Order No").column("order_no")
        ));
        EntityDefinition line = new EntityDefinition("order_line", "sales_order_line", "Order Line", List.of(
                FieldDefinition.string("orderId", "Order").column("order_id"),
                FieldDefinition.string("sku", "SKU")
        ));
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                MODULE,
                "Order",
                List.of(order, line),
                List.of(EntityRelationDefinition.child("lines", "order", "order_line", "orderId"))
        ));
        DynamicRecord first = new DynamicRecord(order);
        first.setId("order-1");
        first.setValue("orderNo", "SO-001");
        DynamicRecord second = new DynamicRecord(order);
        second.setId("order-2");
        second.setValue("orderNo", "SO-002");
        DynamicRecord firstLine = new DynamicRecord(line);
        firstLine.setId("line-1");
        firstLine.setValue("orderId", "order-1");
        firstLine.setValue("sku", "SKU-001");
        Criteria criteria = Criteria.of();
        PageRequest pageRequest = PageRequest.of(1, 50);
        when(recordService.recordsForAction(MODULE, PlatformAction.EXPORT, "dynamic-export")).thenReturn(records);
        when(records.list("order", criteria, pageRequest)).thenReturn(List.of(first, second));
        when(records.list(org.mockito.ArgumentMatchers.eq("order_line"), any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(firstLine));

        ExcelWorkbookPlan plan = executor.export(new DynamicExportCommand(
                descriptor, criteria, pageRequest, List.of()));

        assertThat(plan.sheets()).hasSize(2);
        ExcelSheetPlan main = plan.sheets().getFirst();
        assertThat(main.rows()).containsExactly(
                List.of("order-1", "SO-001"),
                List.of("order-2", "SO-002")
        );
        ExcelSheetPlan child = plan.sheets().get(1);
        assertThat(child.main()).isFalse();
        assertThat(child.entityAlias()).isEqualTo("order_line");
        assertThat(child.rows()).containsExactly(
                List.of("order-1", "order-1", "SKU-001"),
                Arrays.asList("order-2", null, null)
        );
    }
}
