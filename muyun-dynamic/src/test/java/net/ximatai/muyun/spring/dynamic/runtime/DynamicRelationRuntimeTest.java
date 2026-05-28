package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.model.title.TitleField;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicRelationRuntimeTest {
    private static final String SCHEMA = "public";
    private static final String MODULE = "sales.invoice";

    @Test
    void shouldInsertDynamicChildrenThroughSharedChildRelationAbility() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), anyString(), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");
        DynamicRecord line = runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-001");
        invoice.setChildren("lines", List.of(line));

        String id = invoiceService.insert(invoice);

        ArgumentCaptor<String> table = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, times(2)).insertItem(eq(SCHEMA), table.capture(), body.capture());
        assertThat(id).hasSize(32);
        assertThat(table.getAllValues()).containsExactly("app_invoice", "app_invoice_line");
        assertThat(body.getAllValues().get(1))
                .containsEntry("title", "L-001")
                .containsEntry("invoice_id", id)
                .containsEntry("deleted", Boolean.FALSE);
    }

    @Test
    void shouldInsertDynamicChildrenWithCurrentTenant() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), anyString(), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");
        DynamicRecord line = runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-001");
        invoice.setChildren("lines", List.of(line));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            runtime.entityService(MODULE, "invoice").insert(invoice);
        }

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, times(2)).insertItem(eq(SCHEMA), anyString(), body.capture());
        assertThat(body.getAllValues().get(0)).containsEntry("tenant_id", "tenant-a");
        assertThat(body.getAllValues().get(1)).containsEntry("tenant_id", "tenant-a");
    }

    @Test
    void shouldAutoPopulateAndCascadeDeleteDynamicChildren() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        when(operations.patchUpdateItem(eq(SCHEMA), anyString(), anyString(), anyMap())).thenReturn(1);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");

        DynamicRecord selected = invoiceService.select("invoice-1");
        int deleted = invoiceService.delete("invoice-1");

        assertThat(selected.getChildren("lines"))
                .hasSize(1)
                .first()
                .extracting(child -> child.getValue("title"))
                .isEqualTo("L-001");
        assertThat(deleted).isEqualTo(1);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<String> table = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations, times(2)).patchUpdateItemWhere(eq(SCHEMA), table.capture(), body.capture(), where.capture());
        assertThat(table.getAllValues()).containsExactly("app_invoice", "app_invoice_line");
        assertThat(where.getAllValues().get(0)).containsEntry("id", "invoice-1");
        assertThat(where.getAllValues().get(1)).containsEntry("id", "line-1");
        assertThat(body.getAllValues().get(1)).containsEntry("deleted", Boolean.TRUE);
    }

    @Test
    void shouldReplaceDynamicChildrenThroughSharedChildRelationAbility() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"invoice_id\" =")) {
                return containsParam(params, "invoice-1")
                        ? List.of(lineRow("line-1", "L-001", 1), lineRow("line-2", "L-002", 2))
                        : List.of();
            }
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"id\" =")) {
                if (containsParam(params, "line-1")) {
                    return List.of(lineRow("line-1", "L-001", 1));
                }
                if (containsParam(params, "line-2")) {
                    return List.of(lineRow("line-2", "L-002", 2));
                }
                return List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
        when(operations.insertItem(eq(SCHEMA), eq("app_invoice_line"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");
        DynamicRecord retainedLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-001-updated");
        retainedLine.setId("line-1");
        retainedLine.setVersion(1);
        DynamicRecord newLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-003");
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001-updated");
        invoice.setId("invoice-1");
        invoice.setVersion(1);
        invoice.setChildren("lines", List.of(retainedLine, newLine));

        int updated = invoiceService.update(invoice);

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<String> table = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations, times(3)).patchUpdateItemWhere(eq(SCHEMA), table.capture(), body.capture(), where.capture());
        assertThat(table.getAllValues()).containsExactly("app_invoice", "app_invoice_line", "app_invoice_line");
        assertThat(body.getAllValues().get(1))
                .containsEntry("title", "L-001-updated")
                .containsEntry("invoice_id", "invoice-1");
        assertThat(body.getAllValues().get(2))
                .containsEntry("deleted", Boolean.TRUE);
        assertThat(where.getAllValues().get(2)).containsEntry("id", "line-2");

        ArgumentCaptor<Map<String, Object>> inserted = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq("app_invoice_line"), inserted.capture());
        assertThat(inserted.getValue())
                .containsEntry("title", "L-003")
                .containsEntry("invoice_id", "invoice-1")
                .containsEntry("deleted", Boolean.FALSE);
    }

    @Test
    void shouldRejectDuplicateAndForeignDynamicChildIdsOnReplace() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceAndLineRows(operations);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");
        DynamicRecord retainedLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-001");
        retainedLine.setId("line-1");
        retainedLine.setVersion(1);
        DynamicRecord foreignLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "Foreign");
        foreignLine.setId("line-foreign");
        foreignLine.setVersion(1);
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");
        invoice.setId("invoice-1");
        invoice.setVersion(1);

        invoice.setChildren("lines", List.of(retainedLine, retainedLine));
        assertThatThrownBy(() -> invoiceService.update(invoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Duplicate child id");

        invoice.setChildren("lines", List.of(foreignLine));
        assertThatThrownBy(() -> invoiceService.update(invoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("does not belong to parent");
    }

    @Test
    void shouldRejectDuplicateAndForeignDynamicChildIdsOnInsert() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceAndLineRows(operations);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");
        DynamicRecord duplicateLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "Duplicate");
        duplicateLine.setId("line-new");
        duplicateLine.setVersion(1);
        DynamicRecord foreignLine = runtime.newRecord(MODULE, "invoice_line").setValue("title", "Existing");
        foreignLine.setId("line-foreign");
        foreignLine.setVersion(1);
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");

        invoice.setChildren("lines", List.of(duplicateLine, duplicateLine));
        assertThatThrownBy(() -> invoiceService.insert(invoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Duplicate child id");

        invoice.setChildren("lines", List.of(foreignLine));
        assertThatThrownBy(() -> invoiceService.insert(invoice))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("does not belong to parent");
    }

    @Test
    void shouldAutoPopulateDynamicChildrenWithSortCapabilityOrderSql() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"invoice_id\" =")) {
                return containsParam(params, "invoice-1")
                        ? List.of(lineRow("line-1", "L-001", 1), lineRow("line-2", "L-002", 2))
                        : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
        DynamicEntityService invoiceService = new DynamicRecordRuntime(operations)
                .register(sortableInvoiceModule())
                .entityService(MODULE, "invoice");

        DynamicRecord selected = invoiceService.select("invoice-1");

        assertThat(selected.getChildren("lines"))
                .extracting(child -> child.getValue("title"))
                .containsExactly("L-001", "L-002");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(statement -> assertThat(statement)
                .contains("\"app_invoice_line\"")
                .contains("\"invoice_id\" =")
                .contains("ORDER BY \"sort_order\" ASC"));
    }

    @Test
    void shouldApplyTenantScopeWhenPopulatingAndCascadingDynamicChildren() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        when(operations.patchUpdateItem(eq(SCHEMA), anyString(), anyString(), anyMap())).thenReturn(1);
        DynamicEntityService invoiceService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            invoiceService.select("invoice-1");
            invoiceService.delete("invoice-1");
        }

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).allSatisfy(statement -> assertThat(statement).contains("\"tenant_id\" ="));
    }

    @Test
    void shouldReloadDynamicChildrenWhenParentRecordHitsCache() {
        IDatabaseOperations<Object> operations = operations();
        AtomicReference<String> lineTitle = new AtomicReference<>("L-001");
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"invoice_id\" =")) {
                return params.containsValue("invoice-1") ? List.of(lineRow(lineTitle.get())) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return params.containsValue("invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
        DynamicEntityService invoiceService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice");

        DynamicRecord first = invoiceService.select("invoice-1");
        lineTitle.set("L-002");
        DynamicRecord second = invoiceService.select("invoice-1");

        assertThat(first.getChildren("lines"))
                .first()
                .extracting(child -> child.getValue("title"))
                .isEqualTo("L-001");
        assertThat(second.getChildren("lines"))
                .first()
                .extracting(child -> child.getValue("title"))
                .isEqualTo("L-002");
    }

    @Test
    void shouldInvalidateDynamicReferrerCacheWhenReferenceTargetChanges() {
        IDatabaseOperations<Object> operations = operations();
        AtomicReference<String> lineTitle = new AtomicReference<>("L-001");
        AtomicReference<String> invoiceTitle = new AtomicReference<>("I-001");
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"id\" =")) {
                return params.containsValue("line-1") ? List.of(lineRow(lineTitle.get())) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow(invoiceTitle.get())) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" IN")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow(invoiceTitle.get())) : List.of();
            }
            return List.of();
        });
        when(operations.patchUpdateItemWhere(eq(SCHEMA), anyString(), anyMap(), anyMap())).thenReturn(1);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicEntityService invoiceService = runtime.entityService(MODULE, "invoice");
        DynamicEntityService lineService = runtime.entityService(MODULE, "invoice_line");

        DynamicRecord first = lineService.select("line-1");
        lineTitle.set("L-002");
        invoiceTitle.set("I-002");
        DynamicRecord invoice = new DynamicRecord(invoiceEntity()).setValue("title", "I-002");
        invoice.setId("invoice-1");
        invoice.setVersion(1);
        invoiceService.update(invoice);
        DynamicRecord second = lineService.select("line-1");

        assertThat(first.getValue("title")).isEqualTo("L-001");
        assertThat(first.getValue("invoiceDisplayTitle")).isEqualTo("I-001");
        assertThat(second.getValue("title")).isEqualTo("L-002");
        assertThat(second.getValue("invoiceDisplayTitle")).isEqualTo("I-002");
    }

    @Test
    void shouldCollectDynamicReferenceIdsByMetadata() {
        IDatabaseOperations<Object> operations = operations();
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice_line");
        DynamicRecord line = new DynamicRecord(invoiceLineEntity())
                .setValue("invoiceId", "invoice-1")
                .setValue("title", "L-001");

        assertThat(lineService.collectReferenceIdsByTarget(line))
                .containsEntry(ReferenceTarget.of("sales.invoice", "invoice"), Set.of("invoice-1"));
    }

    @Test
    void shouldCollectDynamicManyReferenceIdsByMetadata() {
        IDatabaseOperations<Object> operations = operations();
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(manyReferenceInvoiceModule())
                .entityService(MODULE, "invoice_line");
        DynamicRecord line = new DynamicRecord(invoiceLineEntity())
                .setValue("invoiceId", "invoice-1, invoice-2, invoice-1")
                .setValue("title", "L-001");

        assertThat(lineService.collectReferenceIdsByTarget(line))
                .containsEntry(ReferenceTarget.of("sales.invoice", "invoice"), Set.of("invoice-1", "invoice-2"));
        assertThat(manyReferenceInvoiceModule().references())
                .extracting(reference -> reference.plan().cardinality())
                .containsExactly(ReferenceCardinality.MANY);
    }

    @Test
    void shouldCompressManyDynamicReferenceProjectionValuesToExistingNonNullTargets() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" IN")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(manyProjectionInvoiceModule())
                .entityService(MODULE, "invoice_line");
        DynamicRecord line = new DynamicRecord(invoiceLineEntity())
                .setValue("invoiceId", "invoice-1, missing-invoice")
                .setValue("title", "L-001");

        lineService.afterReferenceSelect(line);

        assertThat(line.getValue("invoiceDisplayTitle")).isEqualTo(List.of("I-001"));
    }

    @Test
    void shouldCompileDynamicChildRelationToPlan() {
        assertThat(invoiceModule().relations())
                .extracting(EntityRelationDefinition::plan)
                .containsExactly(new net.ximatai.muyun.spring.ability.child.ChildPlan(
                        "lines",
                        "invoice",
                        "invoice_line",
                        "invoiceId",
                        true,
                        true
                ));
    }

    @Test
    void shouldNormalizeDynamicReferenceDefinitionDefaults() {
        EntityReferenceDefinition reference = new EntityReferenceDefinition(
                "invoice_line",
                "invoiceId",
                ReferenceTarget.of("sales.invoice", "invoice").qualifiedName(),
                null,
                false,
                null
        );

        assertThat(reference.cardinality()).isEqualTo(ReferenceCardinality.ONE);
        assertThat(reference.titleOutputField()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDynamicReferenceTitleByMetadata() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice_line");

        DynamicRecord line = lineService.select("line-1");

        assertThat(line.getValue("invoiceTitle")).isEqualTo("I-001");
        assertThat(line.getValue("invoiceDisplayTitle")).isEqualTo("I-001");
        assertThat(line.getValues()).doesNotContainKey("invoiceTitle");
        assertThat(line.getValues()).doesNotContainKey("invoiceDisplayTitle");
    }

    @Test
    void shouldPopulateDynamicReferenceProjectionWithoutAutoTitle() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(projectionOnlyInvoiceModule())
                .entityService(MODULE, "invoice_line");

        DynamicRecord line = lineService.select("line-1");

        assertThat(line.getValue("invoiceDisplayTitle")).isEqualTo("I-001");
        assertThat(line.getValues()).doesNotContainKey("invoiceDisplayTitle");
    }

    @Test
    void shouldAllowNullDynamicReferenceProjectionValues() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"id\" =")) {
                return params.containsValue("line-1") ? List.of(lineRow()) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" IN")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRowWithNullTitle()) : List.of();
            }
            return List.of();
        });
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(projectionOnlyInvoiceModule())
                .entityService(MODULE, "invoice_line");

        DynamicRecord line = lineService.select("line-1");

        assertThat(line.getValue("invoiceDisplayTitle")).isNull();
        assertThat(line.getValues()).doesNotContainKey("invoiceDisplayTitle");
    }

    @Test
    void shouldPopulateDynamicReferenceTitleBeforeLifecycleAfterSelect() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        AtomicReference<Object> lifecycleTitle = new AtomicReference<>();
        DynamicRecordLifecycle lifecycle = new DynamicRecordLifecycle() {
            @Override
            public void afterSelect(DynamicRecord record) {
                lifecycleTitle.set(record.getValue("invoiceTitle"));
            }
        };
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice_line", lifecycle);

        lineService.select("line-1");

        assertThat(lifecycleTitle).hasValue("I-001");
    }

    @Test
    void shouldResolveDynamicReferenceByRawTargetRecord() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        AtomicInteger invoiceAfterSelectCount = new AtomicInteger();
        AtomicReference<DynamicEntityService> invoiceService = new AtomicReference<>();
        AtomicReference<DynamicEntityService> lineService = new AtomicReference<>();
        ModuleDefinition module = invoiceModule();
        invoiceService.set(new DynamicEntityService(
                new DynamicRecordDao(operations, invoiceEntity()),
                MODULE,
                new DynamicRecordLifecycle() {
                    @Override
                    public void afterSelect(DynamicRecord record) {
                        invoiceAfterSelectCount.incrementAndGet();
                    }
                },
                module,
                entityCode -> "invoice_line".equals(entityCode) ? lineService.get() : invoiceService.get()
        ));
        lineService.set(new DynamicEntityService(
                new DynamicRecordDao(operations, invoiceLineEntity()),
                MODULE,
                DynamicRecordLifecycle.NONE,
                module,
                entityCode -> "invoice".equals(entityCode) ? invoiceService.get() : lineService.get()
        ));

        DynamicRecord line = lineService.get().select("line-1");

        assertThat(line.getValue("invoiceTitle")).isEqualTo("I-001");
        assertThat(line.getValue("invoiceDisplayTitle")).isEqualTo("I-001");
        assertThat(invoiceAfterSelectCount).hasValue(0);

        invoiceService.get().select("invoice-1");
        assertThat(invoiceAfterSelectCount).hasValue(1);
    }

    @Test
    void shouldResolveDynamicReferenceTitleWithoutAutoPopulatingChildren() {
        IDatabaseOperations<Object> operations = operations();
        stubInvoiceRows(operations);
        DynamicEntityService invoiceService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice");

        assertThat(invoiceService.title("invoice-1")).isEqualTo("I-001");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).noneSatisfy(value -> assertThat(value).contains("\"app_invoice_line\""));
    }

    @Test
    void shouldRejectReferenceCollectionForDifferentDynamicEntity() {
        IDatabaseOperations<Object> operations = operations();
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice_line");

        assertThatThrownBy(() -> lineService.collectReferenceIdsByTarget(new DynamicRecord(invoiceEntity())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entity mismatch");
    }

    @Test
    void shouldRejectUnknownDynamicChildRelationPayload() {
        IDatabaseOperations<Object> operations = operations();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");
        invoice.setChildren("unknown_lines", List.of(runtime.newRecord(MODULE, "invoice_line").setValue("title", "L-001")));

        assertThatThrownBy(() -> runtime.entityService(MODULE, "invoice").insert(invoice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown dynamic child relation");
    }

    @Test
    void shouldRejectMismatchedDynamicChildEntityPayload() {
        IDatabaseOperations<Object> operations = operations();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations).register(invoiceModule());
        DynamicRecord invoice = runtime.newRecord(MODULE, "invoice").setValue("title", "I-001");
        invoice.setChildren("lines", List.of(runtime.newRecord(MODULE, "invoice").setValue("title", "Wrong")));

        assertThatThrownBy(() -> runtime.entityService(MODULE, "invoice").insert(invoice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamic child entity mismatch");
    }

    @Test
    void shouldRejectInvalidDynamicRelationMetadata() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "missingField"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invoice_line.missingField");
    }

    @Test
    void shouldRejectNonStringDynamicRelationForeignKeyMetadata() {
        EntityDefinition invalidLine = new EntityDefinition(
                "invoice_line",
                "app_invoice_line",
                "Invoice Line",
                List.of(
                        FieldDefinition.integer("invoiceId", "Invoice").column("invoice_id").required().indexed(),
                        FieldDefinition.titleField().required()
                )
        ).withCapabilities(EntityCapability.REFERENCE);
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invalidLine),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("relation child foreign key field must be STRING")
                .hasMessageContaining("invoice_line.invoiceId");
    }

    @Test
    void shouldRejectInvalidDynamicReferenceTargetMetadata() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", "sales.invoice"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("reference target module alias");
    }

    @Test
    void shouldRejectUnparseableDynamicReferenceTargetMetadata() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", "sales"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid reference target qualified name");
    }

    @Test
    void shouldRejectDynamicReferenceAutoTitleAcrossModules() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("other.invoice", "invoice"))
                        .withAutoTitle("invoiceTitle"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("same module target");
    }

    @Test
    void shouldRejectUnknownSameModuleDynamicReferenceTarget() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "missing_invoice")))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("reference target entity");
    }

    @Test
    void shouldRejectDynamicReferenceAutoTitleFieldConflicts() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .withAutoTitle("title"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("title output field conflicts");
    }

    @Test
    void shouldRejectDynamicReferenceProjectionAcrossModules() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("other.invoice", "invoice"))
                        .withProjection("title", "invoiceDisplayTitle"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("projection requires same module target");
    }

    @Test
    void shouldRejectDynamicReferenceProjectionFieldConflicts() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .withProjection("title", "title"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("projection output field conflicts");
    }

    @Test
    void shouldRejectDuplicateDynamicReferenceOutputFields() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .withAutoTitle("invoiceDisplay")
                        .withProjection("title", "invoiceDisplay"))
        );

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("reference output field");
    }

    @Test
    void shouldAllowSameRelationCodeOnDifferentDynamicParentEntities() {
        EntityDefinition payment = new EntityDefinition(
                "payment",
                "app_payment",
                "Payment",
                List.of(FieldDefinition.titleField().required())
        ).withCapabilities(EntityCapability.REFERENCE);
        EntityDefinition paymentLine = new EntityDefinition(
                "payment_line",
                "app_payment_line",
                "Payment Line",
                List.of(
                        FieldDefinition.string("paymentId", "Payment").column("payment_id").length(64).required(),
                        FieldDefinition.titleField().required()
                )
        ).withCapabilities(EntityCapability.REFERENCE);
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity(), payment, paymentLine),
                List.of(
                        EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId"),
                        EntityRelationDefinition.child("lines", "payment", "payment_line", "paymentId")
                )
        );

        new ModuleDefinitionValidator().validate(module);
    }

    private void stubInvoiceRows(IDatabaseOperations<Object> operations) {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"id\" =")) {
                return params.containsValue("line-1") ? List.of(lineRow()) : List.of();
            }
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"invoice_id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(lineRow()) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" IN")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
    }

    private boolean containsParam(Map<String, Object> params, Object expected) {
        return params.values().stream().anyMatch(value -> value instanceof Iterable<?> values
                ? containsIterable(values, expected)
                : expected.equals(value));
    }

    private boolean containsIterable(Iterable<?> values, Object expected) {
        for (Object value : values) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private ModuleDefinition invoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent()),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .withAutoTitle("invoiceTitle")
                        .withProjection("title", "invoiceDisplayTitle"))
        );
    }

    private ModuleDefinition sortableInvoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), sortableInvoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent())
        );
    }

    private ModuleDefinition manyReferenceInvoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent()),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice")).many())
        );
    }

    private ModuleDefinition projectionOnlyInvoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent()),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .withProjection("title", "invoiceDisplayTitle"))
        );
    }

    private ModuleDefinition manyProjectionInvoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice"))
                        .many()
                        .withProjection("title", "invoiceDisplayTitle"))
        );
    }

    private EntityDefinition invoiceEntity() {
        return new EntityDefinition(
                "invoice",
                "app_invoice",
                "Invoice",
                List.of(FieldDefinition.titleField().required())
        ).withCapabilities(EntityCapability.REFERENCE);
    }

    private EntityDefinition invoiceLineEntity() {
        return new EntityDefinition(
                "invoice_line",
                "app_invoice_line",
                "Invoice Line",
                List.of(
                        FieldDefinition.string("invoiceId", "Invoice").column("invoice_id").length(64).required().indexed(),
                        FieldDefinition.titleField().required()
                )
        ).withCapabilities(EntityCapability.REFERENCE);
    }

    private EntityDefinition sortableInvoiceLineEntity() {
        return new EntityDefinition(
                "invoice_line",
                "app_invoice_line",
                "Invoice Line",
                List.of(
                        FieldDefinition.string("invoiceId", "Invoice").column("invoice_id").length(64).required().indexed(),
                        FieldDefinition.titleField().required(),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(EntityCapability.REFERENCE, EntityCapability.SORT);
    }

    private Map<String, Object> invoiceRow() {
        return invoiceRow("I-001");
    }

    private Map<String, Object> invoiceRow(String title) {
        return Map.of(
                "id", "invoice-1",
                "tenant_id", "tenant-a",
                "title", title,
                "deleted", Boolean.FALSE,
                "version", 1
        );
    }

    private Map<String, Object> invoiceRowWithNullTitle() {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", "invoice-1");
        row.put("tenant_id", "tenant-a");
        row.put("title", null);
        row.put("deleted", Boolean.FALSE);
        row.put("version", 1);
        return row;
    }

    private Map<String, Object> lineRow() {
        return lineRow("L-001");
    }

    private Map<String, Object> lineRow(String title) {
        return Map.of(
                "id", "line-1",
                "tenant_id", "tenant-a",
                "invoice_id", "invoice-1",
                "title", title,
                "deleted", Boolean.FALSE,
                "version", 1
        );
    }

    private Map<String, Object> lineRow(String id, String title, Integer sortOrder) {
        return Map.of(
                "id", id,
                "tenant_id", "tenant-a",
                "invoice_id", "invoice-1",
                "title", title,
                "sort_order", sortOrder,
                "deleted", Boolean.FALSE,
                "version", 1
        );
    }

    private void stubInvoiceAndLineRows(IDatabaseOperations<Object> operations) {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"invoice_id\" =")) {
                return containsParam(params, "invoice-1")
                        ? List.of(lineRow("line-1", "L-001", 1), lineRow("line-2", "L-002", 2))
                        : List.of();
            }
            if (sql.contains("\"app_invoice_line\"") && sql.contains("\"id\" =")) {
                if (containsParam(params, "line-1")) {
                    return List.of(lineRow("line-1", "L-001", 1));
                }
                if (containsParam(params, "line-2")) {
                    return List.of(lineRow("line-2", "L-002", 2));
                }
                if (containsParam(params, "line-foreign")) {
                    return List.of(lineRow("line-foreign", "Foreign", 1));
                }
                return List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return containsParam(params, "invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(1);
        return operations;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
