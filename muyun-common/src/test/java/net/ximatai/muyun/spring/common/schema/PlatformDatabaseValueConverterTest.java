package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformDatabaseValueConverterTest {

    private final PlatformDatabaseValueConverter converter = new PlatformDatabaseValueConverter();

    @Test
    void shouldConvertCodeTitleEnumByCode() {
        assertThat(converter.toDatabaseValue(Status.ACTIVE)).isEqualTo("active");
        assertThat(converter.fromDatabaseValue("inactive", Status.class)).isEqualTo(Status.INACTIVE);
    }

    @Test
    void shouldFallbackToDatabaseDefaultConverter() {
        assertThat(converter.toDatabaseValue(PlainStatus.ACTIVE)).isEqualTo("ACTIVE");
        assertThat(converter.fromDatabaseValue("INACTIVE", PlainStatus.class)).isEqualTo(PlainStatus.INACTIVE);
    }

    private enum Status implements CodeTitleEnum {
        ACTIVE("active", "Active"),
        INACTIVE("inactive", "Inactive");

        private final String code;
        private final String title;

        Status(String code, String title) {
            this.code = code;
            this.title = title;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private enum PlainStatus {
        ACTIVE,
        INACTIVE
    }
}
