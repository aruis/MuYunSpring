package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

        ArgumentCaptor<String> table = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, times(2)).patchUpdateItem(eq(SCHEMA), table.capture(), id.capture(), body.capture());
        assertThat(table.getAllValues()).containsExactly("app_invoice", "app_invoice_line");
        assertThat(id.getAllValues()).containsExactly("invoice-1", "line-1");
        assertThat(body.getAllValues().get(1)).containsEntry("deleted", Boolean.TRUE);
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
    void shouldCollectDynamicReferenceIdsByMetadata() {
        IDatabaseOperations<Object> operations = operations();
        DynamicEntityService lineService = new DynamicRecordRuntime(operations)
                .register(invoiceModule())
                .entityService(MODULE, "invoice_line");
        DynamicRecord line = new DynamicRecord(invoiceLineEntity())
                .setValue("invoiceId", "invoice-1")
                .setValue("title", "L-001");

        assertThat(lineService.collectReferenceIdsBySourceNamespace(line))
                .containsEntry("sales.invoice.invoice", Set.of("invoice-1"));
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

        assertThatThrownBy(() -> lineService.collectReferenceIdsBySourceNamespace(new DynamicRecord(invoiceEntity())))
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
                return params.containsValue("invoice-1") ? List.of(lineRow()) : List.of();
            }
            if (sql.contains("\"app_invoice\"") && sql.contains("\"id\" =")) {
                return params.containsValue("invoice-1") ? List.of(invoiceRow()) : List.of();
            }
            return List.of();
        });
    }

    private ModuleDefinition invoiceModule() {
        return new ModuleDefinition(
                MODULE,
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent()),
                List.of(EntityReferenceDefinition.from("invoice_line", "invoiceId", "sales.invoice.invoice"))
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

    private Map<String, Object> invoiceRow() {
        return Map.of(
                "id", "invoice-1",
                "title", "I-001",
                "deleted", Boolean.FALSE,
                "version", 1
        );
    }

    private Map<String, Object> lineRow() {
        return lineRow("L-001");
    }

    private Map<String, Object> lineRow(String title) {
        return Map.of(
                "id", "line-1",
                "invoice_id", "invoice-1",
                "title", title,
                "deleted", Boolean.FALSE,
                "version", 1
        );
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
