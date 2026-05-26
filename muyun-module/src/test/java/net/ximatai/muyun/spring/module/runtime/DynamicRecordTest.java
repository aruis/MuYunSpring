package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicRecordTest {

    @Test
    void shouldAcceptValuesDefinedByEntityDefinition() {
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN)
                .setValue("signed_at", Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(record.getValue("code")).isEqualTo("C-001");
        assertThat(record.getValue("amount")).isEqualTo(BigDecimal.TEN);
        assertThat(record.getValues()).containsOnlyKeys("code", "amount", "signed_at");
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

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("code", "code", FieldType.STRING, "Code").length(64).asRequired(),
                        new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount").precision(18, 2),
                        new FieldDefinition("signed_at", "signed_at", FieldType.TIMESTAMP, "Signed At")
                )
        );
    }
}
