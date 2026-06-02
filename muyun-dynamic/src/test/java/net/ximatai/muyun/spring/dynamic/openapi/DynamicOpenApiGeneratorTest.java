package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicOpenApiGeneratorTest {
    private final DynamicOpenApiGenerator generator = new DynamicOpenApiGenerator();

    @Test
    void shouldGenerateStableDynamicModuleOpenApiDocument() {
        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(module()));

        assertThat(document.moduleAlias()).isEqualTo("sales.contract");
        assertThat(document.basePath()).isEqualTo("/sales.contract");
        assertThat(document.operations())
                .extracting(DynamicOpenApiDocument.Operation::path)
                .contains(
                        "/sales.contract/describe",
                        "/sales.contract/openapi",
                        "/sales.contract/query",
                        "/sales.contract/view/{recordId}",
                        "/sales.contract/insert",
                        "/sales.contract/update/{recordId}",
                        "/sales.contract/delete/{recordId}",
                        "/sales.contract/actions",
                        "/sales.contract/actions/{recordId}",
                        "/sales.contract/export",
                        "/sales.contract/submit/{recordId}",
                        "/sales.contract/archive/batch",
                        "/sales.contract/preview",
                        "/sales.contract/preview/{recordId}",
                        "/sales.contract/preview/batch",
                        "/sales.contract/references/customerId/resolve"
                );
        assertThat(document.operations().stream()
                .filter(operation -> operation.path().equals("/sales.contract/submit/{recordId}"))
                .findFirst())
                .get()
                .satisfies(operation -> {
                    assertThat(operation.method()).isEqualTo("POST");
                    assertThat(operation.operationId()).isEqualTo("salesContractRecordSubmit");
                    assertThat(operation.requestSchema()).isEqualTo("DynamicWebActionRequest");
                    assertThat(operation.responseSchema()).isEqualTo("DynamicWebActionExecutionResponse");
                    assertThat(operation.actionCode()).isEqualTo("submit");
                    assertThat(operation.errorCodes()).contains("DYNAMIC_ACTION_FAILED", "DYNAMIC_CONFLICT");
                });
        assertThat(document.operations().stream()
                .filter(operation -> operation.path().equals("/sales.contract/insert"))
                .findFirst())
                .get()
                .satisfies(operation -> {
                    assertThat(operation.requestSchema()).isEqualTo("DynamicRecordPayload");
                    assertThat(operation.responseSchema()).isEqualTo("RecordIdResponse");
                });
        assertThat(document.operations().stream()
                .filter(operation -> operation.path().equals("/sales.contract/view/{recordId}"))
                .findFirst())
                .get()
                .extracting(DynamicOpenApiDocument.Operation::responseSchema)
                .isEqualTo("DynamicRecordResponse");
    }

    @Test
    void shouldGenerateDefaultCrudModuleWithoutExposingInternalStandardActions() {
        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(
                new ModuleDefinition("sales.contract", "Contract", List.of(contractEntity()))));

        assertThat(document.operations())
                .extracting(DynamicOpenApiDocument.Operation::path)
                .contains("/sales.contract/query", "/sales.contract/insert")
                .doesNotContain("/sales.contract/create", "/sales.contract/select/{recordId}",
                        "/sales.contract/queryCriteria", "/sales.contract/count");
    }

    @Test
    void shouldNotExposeCustomActionsThatConflictWithReservedWebPaths() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        action("openapi", "OpenAPI", EntityActionLevel.LIST, EntityActionStyle.NORMAL),
                        action("query", "Query", EntityActionLevel.LIST, EntityActionStyle.NORMAL)
                )
        );

        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(module));

        assertThat(document.operations())
                .extracting(DynamicOpenApiDocument.Operation::path)
                .contains("/sales.contract/openapi", "/sales.contract/query")
                .doesNotContain("/sales.contract/openapi/{recordId}", "/sales.contract/query/{recordId}");
        assertThat(document.operations().stream()
                .filter(operation -> operation.path().equals("/sales.contract/openapi")))
                .singleElement()
                .extracting(DynamicOpenApiDocument.Operation::actionCode)
                .isNull();
    }

    @Test
    void shouldGenerateRecordSchemaFromDynamicFields() {
        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(module()));
        DynamicOpenApiDocument.Schema record = document.schemas().get("ContractRecord");
        DynamicOpenApiDocument.Schema schema = document.schemas().get("ContractValues");

        assertThat(record.properties().get("values").type()).isEqualTo("ContractValues");
        assertThat(schema.required()).contains("code", "amount");
        assertThat(schema.properties().get("code"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("string");
                    assertThat(property.required()).isTrue();
                });
        assertThat(schema.properties().get("amount"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("number");
                    assertThat(property.format()).isEqualTo("decimal");
                });
        assertThat(schema.properties().get("status"))
                .satisfies(property -> {
                    assertThat(property.optionSourceType()).isEqualTo("dictionary");
                    assertThat(property.optionSource()).isEqualTo("sales.contract_status");
                    assertThat(property.multiple()).isFalse();
                });
        assertThat(schema.properties().get("tags"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("array");
                    assertThat(property.itemType()).isNull();
                    assertThat(property.multiple()).isTrue();
                    assertThat(property.optionSource()).isEqualTo("sales.contract_tag");
                });
        assertThat(schema.properties().get("customerId"))
                .satisfies(property -> {
                    assertThat(property.referenceModuleAlias()).isEqualTo("sales.customer");
                    assertThat(property.referenceEntityAlias()).isEqualTo("customer");
                });
        assertThat(schema.properties().get("signedAt"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("string");
                    assertThat(property.format()).isEqualTo("date-time");
                    assertThat(property.companionFields()).containsExactly("signedAtTimeZone");
                });
    }

    @Test
    void shouldExposeDynamicWebErrorSchemas() {
        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(module()));

        assertThat(document.schemas().get("DynamicQueryRequest").properties().get("conditions"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("array");
                    assertThat(property.itemType()).isEqualTo("DynamicWebQueryCondition");
                    assertThat(property.companionFields()).isEmpty();
                });
        assertThat(document.errors())
                .containsEntry("DYNAMIC_BAD_REQUEST",
                        new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_BAD_REQUEST", 400, "DynamicWebError"))
                .containsEntry("DYNAMIC_ACTION_FAILED",
                        new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_ACTION_FAILED", 400, "DynamicWebActionError"))
                .containsEntry("DYNAMIC_CONFLICT",
                        new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_CONFLICT", 409, "DynamicWebError"));
        assertThat(document.schemas().get("DynamicWebActionError").properties())
                .containsKeys("code", "status", "message", "traceId", "failureStage", "context");
    }

    @Test
    void shouldExposeWebResponseSchemasForActionAndCrudContracts() {
        DynamicOpenApiDocument document = generator.generate(DynamicModuleDescriptor.from(module()));

        assertThat(document.schemas().get("RecordIdResponse").properties().get("id").type())
                .isEqualTo("string");
        assertThat(document.schemas().get("CountResponse").properties().get("count"))
                .satisfies(property -> {
                    assertThat(property.type()).isEqualTo("integer");
                    assertThat(property.format()).isEqualTo("int32");
                });
        assertThat(document.schemas().get("DynamicWebActionContext").properties())
                .containsKeys("moduleAlias", "actionCode", "recordId", "traceId");
        assertThat(document.schemas().get("DynamicWebActionResultBody").properties())
                .containsKeys("type", "value", "message", "refresh", "redirectTo");
        assertThat(document.schemas().get("DynamicWebActionAvailabilityResponse").properties())
                .containsKeys("action", "available", "message");
        assertThat(document.schemas().get("DynamicWebActionExecutionResponse").properties().get("body").type())
                .isEqualTo("DynamicWebActionResultBody");
        assertThat(document.schemas().get("DynamicWebActionAvailabilityList").items().type())
                .isEqualTo("DynamicWebActionAvailabilityResponse");
    }

    private ModuleDefinition module() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(lineEntity(), contractEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("contract", "customerId", "sales.customer.customer")),
                List.of(),
                List.of(),
                List.of(
                        action("export", "导出", EntityActionLevel.LIST, EntityActionStyle.NORMAL),
                        action("submit", "提交", EntityActionLevel.RECORD, EntityActionStyle.PRIMARY),
                        action("archive", "归档", EntityActionLevel.BATCH, EntityActionStyle.NORMAL),
                        action("preview", "预览", EntityActionLevel.ANY, EntityActionStyle.NORMAL)
                ),
                "contract"
        );
    }

    private EntityActionDefinition action(String code,
                                          String title,
                                          EntityActionLevel level,
                                          EntityActionStyle style) {
        return new EntityActionDefinition("contract", code, EntityActionKind.CUSTOM, title,
                true, level, style, EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                true, false, null, null, null, EntityActionExecutorType.SERVICE, code + "Executor");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.string("status", "Status")
                        .dictionary("sales", "contract_status"),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2).required(),
                FieldDefinition.of("tags", FieldType.JSON, "Tags")
                        .dictionary("sales", "contract_tag", OptionSelectionMode.MULTIPLE),
                FieldDefinition.string("customerId", "Customer"),
                FieldDefinition.zonedTimestamp("signedAt", "Signed At"),
                FieldDefinition.zonedTimestampTimeZone("signedAt", "signed_at")
        ));
    }

    private EntityDefinition lineEntity() {
        return new EntityDefinition("line", "sales_contract_line", "Contract Line", List.of(
                FieldDefinition.string("contractId", "Contract").required(),
                FieldDefinition.string("productName", "Product").length(128).required(),
                FieldDefinition.decimal("quantity", "Quantity").precision(18, 2)
        ));
    }
}
