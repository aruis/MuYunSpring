package net.ximatai.muyun.spring.platform.exchange;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelValueType;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedSheet;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedWorkbook;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeHeaderParser;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocolValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelExchangeProtocolTest {
    private final ExcelExchangeHeaderParser headerParser = new ExcelExchangeHeaderParser();
    private final ExcelExchangeProtocolValidator validator = new ExcelExchangeProtocolValidator();

    @Test
    void shouldDefaultColumnValueTypeAndDropdownOptions() {
        ExcelColumnPlan column = new ExcelColumnPlan("orderNo", "订单号");

        assertThat(column.required()).isFalse();
        assertThat(column.valueType()).isEqualTo(ExcelValueType.TEXT);
        assertThat(column.dropdownOptions()).isEmpty();
    }

    @Test
    void shouldKeepColumnValueTypeAndCopyDropdownOptions() {
        List<String> options = new ArrayList<>(List.of("NEW", "DONE"));
        ExcelColumnPlan column = new ExcelColumnPlan(
                "plannedAt",
                "计划时间",
                ExcelValueType.DATE_TIME,
                true,
                options
        );

        options.add("CANCELLED");

        assertThat(column.required()).isTrue();
        assertThat(column.valueType()).isEqualTo(ExcelValueType.DATE_TIME);
        assertThat(column.dropdownOptions()).containsExactly("NEW", "DONE");
        assertThatThrownBy(() -> column.dropdownOptions().add("CANCELLED"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldComposeAndParseTechnicalFields() {
        String technicalField = ExcelExchangeProtocol.composeTechnicalField("order_main", "orderNo");

        assertThat(technicalField).isEqualTo("order_main.orderNo");
        ExcelExchangeProtocol.TechnicalField parsed = ExcelExchangeProtocol.parseTechnicalField(technicalField);
        assertThat(parsed.entityAlias()).isEqualTo("order_main");
        assertThat(parsed.fieldName()).isEqualTo("orderNo");

        String relateId = ExcelExchangeProtocol.composeTechnicalField("order_main", "relateId");
        assertThat(relateId).isEqualTo("__relateId");
        ExcelExchangeProtocol.TechnicalField parsedRelateId = ExcelExchangeProtocol.parseTechnicalField(relateId);
        assertThat(parsedRelateId.entityAlias()).isNull();
        assertThat(parsedRelateId.fieldName()).isEqualTo("relateId");
    }

    @Test
    void shouldIdentifyInternalExchangeSheets() {
        assertThat(ExcelExchangeProtocol.isInternalSheet(ExcelExchangeProtocol.META_SHEET_NAME)).isTrue();
        assertThat(ExcelExchangeProtocol.isInternalSheet(ExcelExchangeProtocol.OPTIONS_SHEET_NAME)).isTrue();
        assertThat(ExcelExchangeProtocol.isInternalSheet("_exchange_business")).isFalse();
        assertThat(ExcelExchangeProtocol.isInternalSheet("订单")).isFalse();
    }

    @Test
    void shouldRejectInvalidTechnicalField() {
        assertThatThrownBy(() -> ExcelExchangeProtocol.parseTechnicalField("orderNo"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("invalid exchange technical field");
    }

    @Test
    void shouldParseDoubleHeadersIntoParsedSheet() {
        ParsedSheet sheet = headerParser.parse(
                "订单",
                List.of("__relateId", "order_main.orderNo", "order_main.customerName"),
                List.of("关联标识", "订单号", "客户名称"),
                List.of(List.of("R-1", "SO-001", "Acme"))
        );

        assertThat(sheet.sheetName()).isEqualTo("订单");
        assertThat(sheet.entityAlias()).isEqualTo("order_main");
        assertThat(sheet.columns()).hasSize(3);
        assertThat(sheet.columns().get(0).fieldName()).isEqualTo("relateId");
        assertThat(sheet.columns().get(0).entityAlias()).isNull();
        assertThat(sheet.columns().get(1).entityAlias()).isEqualTo("order_main");
        assertThat(sheet.columns().get(1).fieldName()).isEqualTo("orderNo");
        assertThat(sheet.columns().get(1).title()).isEqualTo("订单号");
    }

    @Test
    void shouldRejectHeaderMixingMultipleEntityAliases() {
        assertThatThrownBy(() -> headerParser.parse(
                "订单",
                List.of("__relateId", "order_main.orderNo", "order_line.qty"),
                List.of("关联标识", "订单号", "数量"),
                List.of()
        ))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("multiple entity aliases");
    }

    @Test
    void shouldRejectHeaderWithoutBusinessField() {
        assertThatThrownBy(() -> headerParser.parse(
                "订单",
                List.of("__relateId"),
                List.of("关联标识"),
                List.of()
        ))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("business field");
    }

    @Test
    void shouldValidateWorkbookPlan() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(
                new ExcelWorkbookMeta("1", "crm.order", "view-1", "订单导入", "Asia/Shanghai"),
                List.of(
                        sheet("订单", "order_main", true, "orderNo"),
                        sheet("订单明细", "order_line", false, "lineName", "qty")
                )
        );

        validator.validateWorkbookPlan(plan);
    }

    @Test
    void shouldRejectWorkbookPlanWithoutExactlyOneMainSheet() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(List.of(
                sheet("订单", "order_main", false, "orderNo"),
                sheet("订单明细", "order_line", false, "lineName")
        ));

        assertThatThrownBy(() -> validator.validateWorkbookPlan(plan))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("exactly one main sheet");
    }

    @Test
    void shouldRejectDuplicateSheetNamesInWorkbookPlan() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(List.of(
                sheet("订单", "order_main", true, "orderNo"),
                sheet("订单", "order_line", false, "lineName")
        ));

        assertThatThrownBy(() -> validator.validateWorkbookPlan(plan))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("sheet name duplicated");
    }

    @Test
    void shouldRejectDuplicateFieldsInWorkbookPlan() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(List.of(new ExcelSheetPlan(
                "订单",
                "order_main",
                true,
                List.of(
                        new ExcelColumnPlan("relateId", "关联标识"),
                        new ExcelColumnPlan("orderNo", "订单号"),
                        new ExcelColumnPlan("orderNo", "订单号")
                )
        )));

        assertThatThrownBy(() -> validator.validateWorkbookPlan(plan))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("field duplicated");
    }

    @Test
    void shouldRejectWorkbookPlanSheetWithoutRelateId() {
        ExcelWorkbookPlan plan = new ExcelWorkbookPlan(List.of(new ExcelSheetPlan(
                "订单",
                "order_main",
                true,
                List.of(new ExcelColumnPlan("orderNo", "订单号"))
        )));

        assertThatThrownBy(() -> validator.validateWorkbookPlan(plan))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("__relateId");
    }

    @Test
    void shouldValidateParsedWorkbookTimeZoneAndRelateId() {
        ParsedSheet sheet = headerParser.parse(
                "订单",
                List.of("__relateId", "order_main.orderNo"),
                List.of("关联标识", "订单号"),
                List.of()
        );
        validator.validateParsedWorkbook(new ParsedWorkbook(
                new ExcelWorkbookMeta("1", "crm.order", "view-1", "订单导入", "Asia/Shanghai"),
                List.of(sheet)
        ));

        assertThatThrownBy(() -> validator.validateParsedWorkbook(new ParsedWorkbook(
                new ExcelWorkbookMeta("1", "crm.order", "view-1", "订单导入", "Mars/Base"),
                List.of(sheet)
        )))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("timeZone");

        ParsedSheet missingRelateId = headerParser.parse(
                "订单",
                List.of("order_main.orderNo"),
                List.of("订单号"),
                List.of()
        );
        assertThatThrownBy(() -> validator.validateParsedWorkbook(new ParsedWorkbook(List.of(missingRelateId))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("__relateId");
    }

    private ExcelSheetPlan sheet(String sheetName, String entityAlias, boolean main, String... fields) {
        List<ExcelColumnPlan> columns = new java.util.ArrayList<>();
        columns.add(new ExcelColumnPlan("relateId", "关联标识"));
        for (String field : fields) {
            columns.add(new ExcelColumnPlan(field, field));
        }
        return new ExcelSheetPlan(sheetName, entityAlias, main, columns);
    }
}
