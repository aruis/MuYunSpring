package net.ximatai.muyun.spring.platform.exchange.template;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelValueType;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocolValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicExchangeTemplatePlanBuilderTest {
    private final DynamicExchangeTemplatePlanBuilder builder = new DynamicExchangeTemplatePlanBuilder();

    @Test
    void shouldBuildMainSheetFromDynamicModuleDescriptor() {
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                        FieldDefinition.string("contractNo", "Contract No").required(),
                        FieldDefinition.decimal("amount", "Amount"),
                        FieldDefinition.bool("enabled", "Enabled"),
                        FieldDefinition.of("signedDate", FieldType.DATE, "Signed Date"),
                        FieldDefinition.timestamp("submittedAt", "Submitted At"),
                        FieldDefinition.of("remark", FieldType.TEXT, "")
                )))
        ));

        ExcelWorkbookPlan plan = builder.build(descriptor);

        assertThat(plan.meta().protocolVersion()).isEqualTo(ExcelExchangeProtocol.PROTOCOL_VERSION);
        assertThat(plan.meta().moduleAlias()).isEqualTo("sales.contract");
        assertThat(plan.sheets()).hasSize(1);
        ExcelSheetPlan sheet = plan.sheets().getFirst();
        assertThat(sheet.sheetName()).isEqualTo("Contract");
        assertThat(sheet.entityAlias()).isEqualTo("contract");
        assertThat(sheet.main()).isTrue();
        assertThat(sheet.rows()).isEmpty();
        assertRelateIdColumn(sheet.columns().getFirst());
        assertThat(sheet.columns())
                .extracting(ExcelColumnPlan::fieldName)
                .containsExactly("relateId", "contractNo", "amount", "enabled", "signedDate", "submittedAt", "remark");
        assertThat(sheet.columns().get(1))
                .satisfies(column -> {
                    assertThat(column.title()).isEqualTo("Contract No");
                    assertThat(column.required()).isTrue();
                    assertThat(column.valueType()).isEqualTo(ExcelValueType.TEXT);
                    assertThat(column.dropdownOptions()).isEmpty();
                });
        assertThat(sheet.columns().get(2).valueType()).isEqualTo(ExcelValueType.NUMBER);
        assertThat(sheet.columns().get(3).valueType()).isEqualTo(ExcelValueType.BOOLEAN);
        assertThat(sheet.columns().get(4).valueType()).isEqualTo(ExcelValueType.DATE);
        assertThat(sheet.columns().get(5).valueType()).isEqualTo(ExcelValueType.DATE_TIME);
        assertThat(sheet.columns().get(6).title()).isEqualTo("remark");
        new ExcelExchangeProtocolValidator().validateWorkbookPlan(plan);
    }

    @Test
    void shouldBuildMainAndFirstLevelChildSheets() {
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                "sales.order",
                "Order",
                List.of(
                        new EntityDefinition("order", "sales_order", "Order", List.of(
                                FieldDefinition.string("orderNo", "Order No")
                        )),
                        new EntityDefinition("orderLine", "sales_order_line", "Order Line", List.of(
                                FieldDefinition.string("orderId", "Order"),
                                FieldDefinition.string("sku", "SKU")
                        )),
                        new EntityDefinition("lineBatch", "sales_line_batch", "Line Batch", List.of(
                                FieldDefinition.string("lineId", "Line"),
                                FieldDefinition.string("batchNo", "Batch No")
                        ))
                ),
                List.of(
                        EntityRelationDefinition.child("lines", "order", "orderLine", "orderId"),
                        EntityRelationDefinition.child("batches", "orderLine", "lineBatch", "lineId")
                )
        ));

        ExcelWorkbookPlan plan = builder.build(descriptor);

        assertThat(plan.sheets()).hasSize(2);
        assertThat(plan.sheets()).extracting(ExcelSheetPlan::sheetName)
                .containsExactly("Order", "Order Line");
        ExcelSheetPlan child = plan.sheets().get(1);
        assertThat(child.main()).isFalse();
        assertThat(child.entityAlias()).isEqualTo("orderLine");
        assertRelateIdColumn(child.columns().getFirst());
        assertThat(child.columns())
                .extracting(ExcelColumnPlan::fieldName)
                .containsExactly("relateId", "orderId", "sku");
        new ExcelExchangeProtocolValidator().validateWorkbookPlan(plan);
    }

    @Test
    void shouldSkipZonedTimestampCompanionAndMapOwnerAsDateTimeWithTimeZone() {
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                "sales.meeting",
                "Meeting",
                List.of(new EntityDefinition("meeting", "sales_meeting", "Meeting", List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at"),
                        FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at"),
                        FieldDefinition.string("subject", "Subject")
                )))
        ));

        ExcelSheetPlan sheet = builder.build(descriptor).sheets().getFirst();

        assertThat(sheet.columns())
                .extracting(ExcelColumnPlan::fieldName)
                .containsExactly("relateId", "meetingAt", "subject");
        assertThat(sheet.columns().get(1).valueType()).isEqualTo(ExcelValueType.DATE_TIME_WITH_TIME_ZONE);
    }

    @Test
    void shouldLoadDictionaryOptionsIntoTemplateColumns() {
        DynamicExchangeTemplatePlanBuilder optionBuilder = new DynamicExchangeTemplatePlanBuilder(
                new OptionSourceRegistry(List.of(dictionaryProvider())));
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                        FieldDefinition.string("status", "Status").dictionary("sales", "contract_status")
                )))
        ));

        ExcelSheetPlan sheet = optionBuilder.build(descriptor).sheets().getFirst();

        assertThat(sheet.columns().get(1).fieldName()).isEqualTo("status");
        assertThat(sheet.columns().get(1).dropdownOptions()).containsExactly("draft", "active");
    }

    @Test
    void shouldRejectMissingMainEntity() {
        DynamicModuleDescriptor descriptor = new DynamicModuleDescriptor(
                "sales.contract",
                "Contract",
                "contract",
                List.of(),
                List.of(entity("customer", "Customer")),
                List.of(),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> builder.build(descriptor))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("main entity not found")
                .hasMessageContaining("contract");
    }

    @Test
    void shouldRejectMainEntityWithoutBusinessFields() {
        DynamicModuleDescriptor descriptor = new DynamicModuleDescriptor(
                "sales.contract",
                "Contract",
                "contract",
                List.of(),
                List.of(new DynamicEntityDescriptor(
                        "contract",
                        "Contract",
                        Set.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )),
                List.of(),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> builder.build(descriptor))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("main sheet requires business fields");
    }

    @Test
    void shouldFallbackSheetNameAndDeduplicateChildEntitySheets() {
        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(new ModuleDefinition(
                "sales.order",
                "Order",
                List.of(
                        new EntityDefinition("order", "sales_order", "", List.of(
                                FieldDefinition.string("orderNo", "Order No")
                        )),
                        new EntityDefinition("orderLine", "sales_order_line", "", List.of(
                                FieldDefinition.string("orderId", "Order"),
                                FieldDefinition.string("sku", "SKU")
                        ))
                ),
                List.of(
                        EntityRelationDefinition.child("lines", "order", "orderLine", "orderId"),
                        EntityRelationDefinition.child("attachments", "order", "orderLine", "orderId")
                )
        ));

        ExcelWorkbookPlan plan = builder.build(descriptor);

        assertThat(plan.sheets()).hasSize(2);
        assertThat(plan.sheets()).extracting(ExcelSheetPlan::sheetName)
                .containsExactly("order", "orderLine");
        new ExcelExchangeProtocolValidator().validateWorkbookPlan(plan);
    }

    private DynamicEntityDescriptor entity(String entityAlias, String title) {
        return new DynamicEntityDescriptor(
                entityAlias,
                title,
                Set.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void assertRelateIdColumn(ExcelColumnPlan column) {
        assertThat(column.fieldName()).isEqualTo(ExcelExchangeProtocol.RELATE_ID_FIELD);
        assertThat(column.title()).isEqualTo(ExcelExchangeProtocol.RELATE_ID_TITLE);
        assertThat(column.valueType()).isEqualTo(ExcelValueType.TEXT);
        assertThat(column.required()).isFalse();
    }

    private OptionSourceProvider dictionaryProvider() {
        return new OptionSourceProvider() {
            @Override
            public String sourceType() {
                return OptionBinding.DICTIONARY_SOURCE;
            }

            @Override
            public boolean supports(OptionBinding binding) {
                return binding != null && OptionBinding.DICTIONARY_SOURCE.equals(binding.sourceType());
            }

            @Override
            public OptionSource source(OptionBinding binding) {
                return new OptionSource() {
                    @Override
                    public OptionBinding binding() {
                        return binding;
                    }

                    @Override
                    public List<OptionItem> options(OptionQuery query) {
                        return List.of(
                                new OptionItem("draft", "Draft", true, 1, null),
                                new OptionItem("active", "Active", true, 2, null)
                        );
                    }
                };
            }
        };
    }
}
