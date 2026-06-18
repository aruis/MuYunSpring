package net.ximatai.muyun.spring.dynamic.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldTypeTest {
    @Test
    void shouldExposeTemporalSemanticsForTimeFieldTypes() {
        assertThat(FieldType.DATE.temporalSemantics()).isEqualTo(FieldTemporalSemantics.BUSINESS_DATE);
        assertThat(FieldType.DATE.isBusinessDate()).isTrue();

        assertThat(FieldType.TIMESTAMP.temporalSemantics()).isEqualTo(FieldTemporalSemantics.UTC_INSTANT);
        assertThat(FieldType.TIMESTAMP.isUtcInstant()).isTrue();

        assertThat(FieldType.ZONED_TIMESTAMP.temporalSemantics()).isEqualTo(FieldTemporalSemantics.ZONED_INSTANT);
        assertThat(FieldType.ZONED_TIMESTAMP.isZonedInstant()).isTrue();
    }

    @Test
    void shouldMarkNonTimeFieldTypesAsNonTemporal() {
        assertThat(FieldType.STRING.temporalSemantics()).isEqualTo(FieldTemporalSemantics.NONE);
        assertThat(FieldType.STRING.isTemporal()).isFalse();
        assertThat(FieldType.DECIMAL.isTemporal()).isFalse();
        assertThat(FieldType.JSON.isTemporal()).isFalse();
    }
}
