package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedColumn;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedSheet;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicImportValueConverterTest {
    private final DynamicImportValueConverter converter = new DynamicImportValueConverter();

    @Test
    void shouldConvertSimpleFieldTypesAndSkipBlankConvertedValues() {
        ParsedImportRow row = converter.convert(typePlan(), typeSheet(), List.of(
                "R-1",
                "hello",
                "",
                "{\"a\":1}",
                "12",
                "9,223,372,036,854,775",
                "1,234.50",
                "是",
                "2026-06-08",
                "2026-06-08T01:02:03Z",
                "2026-06-08 09:30:00"
        ), new ImportTemporalContext(java.time.ZoneId.of("Asia/Shanghai")));

        assertThat(row.valuesByFieldName()).containsEntry("textValue", null);
        assertThat(row.convertedValues())
                .containsEntry("stringValue", "hello")
                .containsEntry("jsonValue", "{\"a\":1}")
                .containsEntry("intValue", 12)
                .containsEntry("longValue", 9223372036854775L)
                .containsEntry("decimalValue", new BigDecimal("1234.50"))
                .containsEntry("boolValue", true)
                .containsEntry("dateValue", LocalDate.parse("2026-06-08"))
                .containsEntry("timestampUtcValue", Instant.parse("2026-06-08T01:02:03Z"))
                .containsEntry("timestampLocalValue", Instant.parse("2026-06-08T01:30:00Z"));
        assertThat(row.convertedValues()).doesNotContainKey("textValue");
    }

    @Test
    void shouldRejectIntegerWithDecimalText() {
        assertThatThrownBy(() -> converter.convert(typePlan(), typeSheet(), List.of(
                "R-1", "hello", "", "{}", "12.0", "1", "1", "true", "2026-06-08",
                "2026-06-08T01:02:03Z", "2026-06-08 09:30:00"
        ), ImportTemporalContext.UTC))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("integer value must not contain decimals");
    }

    @Test
    void shouldConvertZonedTimestampWithCompanionTimeZone() {
        ParsedImportRow row = converter.convert(zonedPlan(), zonedSheet(), List.of(
                "R-1", "2026-06-08 09:30:00", "Asia/Shanghai"
        ), ImportTemporalContext.UTC);

        assertThat(row.convertedValues())
                .containsEntry("meetingAt", Instant.parse("2026-06-08T01:30:00Z"))
                .containsEntry("meetingAtTimeZone", "Asia/Shanghai");
    }

    @Test
    void shouldRejectZonedTimestampWithoutCompanionTimeZone() {
        assertThatThrownBy(() -> converter.convert(zonedPlan(), zonedSheet(), List.of(
                "R-1", "2026-06-08 09:30:00", ""
        ), ImportTemporalContext.UTC))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("requires companion timeZone");
    }

    @Test
    void shouldAllowBlankZonedTimestampWithoutCompanionTimeZone() {
        ParsedImportRow row = converter.convert(zonedPlan(), zonedSheet(), List.of(
                "R-1", "", ""
        ), ImportTemporalContext.UTC);

        assertThat(row.convertedValues()).isEmpty();
    }

    @Test
    void shouldRejectZonedTimestampCompanionWithoutOwnerValue() {
        assertThatThrownBy(() -> converter.convert(zonedPlan(), zonedSheet(), List.of(
                "R-1", "", "Asia/Shanghai"
        ), ImportTemporalContext.UTC))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("cannot be provided without value");
    }

    private DynamicImportPlan.SheetPlan typePlan() {
        return new DynamicImportPlan.SheetPlan("Type", "type", "Type", true, "stringValue",
                ImportDuplicateStrategy.ERROR, List.of(
                field("relateId", "关联标识", FieldType.TEXT, true, false, false),
                field("stringValue", "String", FieldType.STRING),
                field("textValue", "Text", FieldType.TEXT),
                field("jsonValue", "Json", FieldType.JSON),
                field("intValue", "Integer", FieldType.INTEGER),
                field("longValue", "Long", FieldType.LONG),
                field("decimalValue", "Decimal", FieldType.DECIMAL),
                field("boolValue", "Boolean", FieldType.BOOLEAN),
                field("dateValue", "Date", FieldType.DATE),
                field("timestampUtcValue", "Timestamp UTC", FieldType.TIMESTAMP),
                field("timestampLocalValue", "Timestamp Local", FieldType.TIMESTAMP)
        ));
    }

    private ParsedSheet typeSheet() {
        return sheet("Type", "type", List.of(
                "relateId",
                "stringValue",
                "textValue",
                "jsonValue",
                "intValue",
                "longValue",
                "decimalValue",
                "boolValue",
                "dateValue",
                "timestampUtcValue",
                "timestampLocalValue"
        ));
    }

    private DynamicImportPlan.SheetPlan zonedPlan() {
        return new DynamicImportPlan.SheetPlan("Meeting", "meeting", "Meeting", true, "meetingAt",
                ImportDuplicateStrategy.ERROR, List.of(
                field("relateId", "关联标识", FieldType.TEXT, true, false, false),
                field("meetingAt", "Meeting At", FieldType.ZONED_TIMESTAMP),
                field("meetingAtTimeZone", "Time Zone", FieldType.STRING, false, false, true)
        ));
    }

    private ParsedSheet zonedSheet() {
        return sheet("Meeting", "meeting", List.of("relateId", "meetingAt", "meetingAtTimeZone"));
    }

    private ParsedSheet sheet(String sheetName, String entityAlias, List<String> fields) {
        java.util.ArrayList<ParsedColumn> columns = new java.util.ArrayList<>();
        for (int index = 0; index < fields.size(); index++) {
            String fieldName = fields.get(index);
            columns.add(new ParsedColumn(index, entityAlias, fieldName,
                    ExcelExchangeProtocol.RELATE_ID_FIELD.equals(fieldName)
                            ? ExcelExchangeProtocol.RELATE_ID_TITLE
                            : fieldName));
        }
        return new ParsedSheet(sheetName, entityAlias, columns, List.of());
    }

    private DynamicImportPlan.FieldPlan field(String fieldName, String title, FieldType type) {
        return field(fieldName, title, type, false, true, false);
    }

    private DynamicImportPlan.FieldPlan field(String fieldName,
                                             String title,
                                             FieldType type,
                                             boolean relateId,
                                             boolean matchKeyCandidate,
                                             boolean companion) {
        return new DynamicImportPlan.FieldPlan("type", fieldName, title, type,
                relateId, matchKeyCandidate, companion);
    }
}
