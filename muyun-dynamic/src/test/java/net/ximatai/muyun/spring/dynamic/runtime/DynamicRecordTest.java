package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicRecordTest {

    @Test
    void shouldAcceptValuesDefinedByEntityDefinition() {
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN)
                .setValue("signedAt", Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(record.getValue("code")).isEqualTo("C-001");
        assertThat(record.getValue("amount")).isEqualTo(BigDecimal.TEN);
        assertThat(record.getValues()).containsOnlyKeys("code", "amount", "signedAt");
    }

    @Test
    void shouldRejectUnknownFieldAndInvalidValueType() {
        DynamicRecord record = new DynamicRecord(contractEntity());

        assertThatThrownBy(() -> record.setValue("unknown", "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown dynamic field");
        assertThatThrownBy(() -> record.getValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown dynamic field");
        assertThatThrownBy(() -> record.setValue("amount", "10.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
    }

    @Test
    void shouldValidateRequiredFieldsBeforeInsert() {
        DynamicRecord record = new DynamicRecord(contractEntity());

        assertThatThrownBy(record::validateForInsert)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required dynamic field is missing: code");
        assertThatThrownBy(() -> record.setValue("code", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required dynamic field must not be null");
    }

    @Test
    void shouldApplyDefaultValuesAndValidateRegex() {
        DynamicRecord record = new DynamicRecord(behaviorEntity());

        record.applyDefaultsForInsert();

        assertThat(record.getValue("status")).isEqualTo("draft");
        assertThatThrownBy(() -> record.setValue("code", "abc-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validationRegex");
        assertThat(new DynamicRecord(behaviorEntity()).setValue("code", "ABC-001").getValue("code"))
                .isEqualTo("ABC-001");
    }

    @Test
    void shouldNormalizeDynamicDateAndTimestampValues() {
        DynamicRecord record = new DynamicRecord(timeEntity())
                .setValue("businessDate", "2026-01-02")
                .setValue("submittedAt", "2026-01-02T09:30:00Z");

        assertThat(record.getValue("businessDate")).isEqualTo(LocalDate.parse("2026-01-02"));
        assertThat(record.getValue("submittedAt")).isEqualTo(Instant.parse("2026-01-02T09:30:00Z"));

        DynamicRecord jdbcRecord = new DynamicRecord(timeEntity())
                .setValue("businessDate", java.sql.Date.valueOf("2026-01-03"))
                .setValue("submittedAt", Instant.parse("2026-01-03T01:00:00Z"));
        assertThat(jdbcRecord.getValue("businessDate")).isEqualTo(LocalDate.parse("2026-01-03"));
        assertThat(jdbcRecord.getValue("submittedAt")).isEqualTo(Instant.parse("2026-01-03T01:00:00Z"));
    }

    @Test
    void shouldRejectTimestampWriteValuesWithoutUtcInstantSemantics() {
        DynamicRecord record = new DynamicRecord(timeEntity());

        assertThatThrownBy(() -> record.setValue("submittedAt", "2026-01-02T09:30:00+08:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
        assertThatThrownBy(() -> record.setValue("submittedAt", "2026-01-02T09:30:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
        assertThatThrownBy(() -> record.setValue("submittedAt", java.sql.Timestamp.from(Instant.parse("2026-01-03T01:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
    }

    @Test
    void shouldApplyDateAndTimestampDefaultValuesAsStableTypes() {
        DynamicRecord record = new DynamicRecord(timeDefaultEntity());

        record.applyDefaultsForInsert();

        assertThat(record.getValue("businessDate")).isEqualTo(LocalDate.parse("2026-01-02"));
        assertThat(record.getValue("submittedAt")).isEqualTo(Instant.parse("2026-01-02T09:30:00Z"));
    }

    @Test
    void shouldNormalizeLoadedDateAndTimestampValuesWithoutRequiredWriteValidation() {
        DynamicRecord record = new DynamicRecord(requiredTimeEntity());

        record.putLoadedValue("businessDate", java.sql.Date.valueOf("2026-01-02"));
        record.putLoadedValue("submittedAt", LocalDateTime.parse("2026-01-02T09:30:00"));

        assertThat(record.getValue("businessDate")).isEqualTo(LocalDate.parse("2026-01-02"));
        assertThat(record.getValue("submittedAt")).isEqualTo(Instant.parse("2026-01-02T09:30:00Z"));

        record.putLoadedValue("businessDate", null);
        assertThat(record.getValue("businessDate")).isNull();
    }

    @Test
    void shouldNormalizeZonedTimestampAndIanaTimeZoneCompanion() {
        DynamicRecord record = new DynamicRecord(zonedTimestampEntity())
                .setValue("meetingAt", "2026-01-02T01:30:00Z")
                .setValue("meetingAtTimeZone", "Asia/Shanghai");

        assertThat(record.getValue("meetingAt")).isEqualTo(Instant.parse("2026-01-02T01:30:00Z"));
        assertThat(record.getValue("meetingAtTimeZone")).isEqualTo("Asia/Shanghai");
        record.validateForInsert();
    }

    @Test
    void shouldRejectZonedTimestampWithoutIanaTimeZoneCompanion() {
        DynamicRecord record = new DynamicRecord(optionalZonedTimestampEntity())
                .setValue("meetingAt", "2026-01-02T01:30:00Z");

        assertThatThrownBy(record::validateForInsert)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field companion is missing");
        assertThatThrownBy(() -> record.setValue("meetingAtTimeZone", "+08:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
        assertThatThrownBy(() -> record.setValue("meetingAtTimeZone", "Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid value type");
    }

    @Test
    void shouldDeepCopyMutableDynamicValues() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", new java.util.ArrayList<>(List.of("A")));
        DynamicRecord record = new DynamicRecord(jsonEntity())
                .setValue("payload", payload);
        record.setTenantId("tenant-a");
        record.setDeletedAt(Instant.parse("2026-01-02T00:00:00Z"));

        DynamicRecord copy = record.copy();
        assertThat(copy.getTenantId()).isEqualTo("tenant-a");
        assertThat(copy.getDeletedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        @SuppressWarnings("unchecked")
        Map<String, Object> copiedPayload = (Map<String, Object>) copy.getValue("payload");
        @SuppressWarnings("unchecked")
        List<String> copiedItems = (List<String>) copiedPayload.get("items");
        copiedItems.add("B");

        @SuppressWarnings("unchecked")
        List<String> originalItems = (List<String>) ((Map<String, Object>) record.getValue("payload")).get("items");
        assertThat(originalItems).containsExactly("A");
        assertThat(copiedItems).containsExactly("A", "B");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.timestamp("signedAt", "Signed At").column("signed_at")
                )
        );
    }

    private EntityDefinition jsonEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.of("payload", FieldType.JSON, "Payload"))
        );
    }

    private EntityDefinition behaviorEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").validationRegex("[A-Z]+-[0-9]+"),
                        FieldDefinition.string("status", "Status").defaultValue("draft")
                )
        );
    }

    private EntityDefinition timeEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.of("businessDate", FieldType.DATE, "Business Date").column("business_date"),
                        FieldDefinition.timestamp("submittedAt", "Submitted At").column("submitted_at")
                )
        );
    }

    private EntityDefinition timeDefaultEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.of("businessDate", FieldType.DATE, "Business Date").column("business_date")
                                .defaultValue("2026-01-02"),
                        FieldDefinition.timestamp("submittedAt", "Submitted At").column("submitted_at")
                                .defaultValue("2026-01-02T09:30:00Z")
                )
        );
    }

    private EntityDefinition requiredTimeEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.of("businessDate", FieldType.DATE, "Business Date").column("business_date").required(),
                        FieldDefinition.timestamp("submittedAt", "Submitted At").column("submitted_at").required()
                )
        );
    }

    private EntityDefinition zonedTimestampEntity() {
        return new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at").required(),
                        FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at").required()
                )
        );
    }

    private EntityDefinition optionalZonedTimestampEntity() {
        return new EntityDefinition(
                "meeting",
                "app_meeting",
                "Meeting",
                List.of(
                        FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").column("meeting_at"),
                        FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at")
                )
        );
    }
}
