package net.ximatai.muyun.spring.common.model.title;

import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleFieldResolverTest {
    @BeforeEach
    void setUp() {
        TitleFieldResolver.clearCacheForTests();
    }

    @Test
    void resolveShouldReadDeclaredTitleField() {
        DemoTitleRecord record = new DemoTitleRecord("Display name");

        assertThat(TitleFieldResolver.resolveFieldName(DemoTitleRecord.class)).contains("name");
        assertThat(TitleFieldResolver.readAsString(record)).isEqualTo("Display name");
    }

    @Test
    void resolveShouldRejectMultipleTitleFieldsAcrossHierarchy() {
        DemoSpecialTitleRecord record = new DemoSpecialTitleRecord("Parent name", "Special name");

        assertThatThrownBy(() -> TitleFieldResolver.resolveFieldName(DemoSpecialTitleRecord.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multiple title fields");
        assertThatThrownBy(() -> TitleFieldResolver.readAsString(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multiple title fields");
    }

    @Test
    void resolveShouldReturnEmptyWhenTitleFieldIsNotDeclared() {
        assertThat(TitleFieldResolver.resolveFieldName(DemoRecordWithoutTitle.class)).isEmpty();
        assertThat(TitleFieldResolver.readAsString(new DemoRecordWithoutTitle())).isNull();
    }

    @Test
    void resolveShouldReadStandardTitledEntityFieldFromHierarchy() {
        DemoStandardTitleRecord record = new DemoStandardTitleRecord();
        record.setTitle("Standard title");

        assertThat(TitleFieldResolver.resolveFieldName(DemoStandardTitleRecord.class)).contains("title");
        assertThat(TitleFieldResolver.readAsString(record)).isEqualTo("Standard title");
    }

    @Test
    void resolveShouldRejectMultipleTitleFieldsOnSameClass() {
        assertThatThrownBy(() -> TitleFieldResolver.resolveFieldName(DemoInvalidTitleRecord.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multiple title fields");
    }

    private static class DemoTitleRecord {
        @TitleField
        private final String name;

        private DemoTitleRecord(String name) {
            this.name = name;
        }
    }

    private static final class DemoSpecialTitleRecord extends DemoTitleRecord {
        @TitleField
        private final String specialName;

        private DemoSpecialTitleRecord(String name, String specialName) {
            super(name);
            this.specialName = specialName;
        }
    }

    private static final class DemoRecordWithoutTitle {
    }

    private static final class DemoStandardTitleRecord extends StandardTitledEntity {
    }

    private static final class DemoInvalidTitleRecord {
        @TitleField
        private String first;

        @TitleField
        private String second;
    }
}
