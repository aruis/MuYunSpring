package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeTitleEnumOptionSourceTest {
    @Test
    void shouldExposeCodeTitleEnumAsOptions() {
        CodeTitleEnumOptionSource<OrderState> source = CodeTitleEnumOptionSource.of(OrderState.class);

        assertThat(source.binding())
                .isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(source.options()).containsExactly(
                new OptionItem("draft", "Draft", true, 1, null),
                new OptionItem("approved", "Approved", true, 2, null)
        );
        assertThat(source.resolve("approved").title()).isEqualTo("Approved");
    }

    private enum OrderState implements CodeTitleEnum {
        DRAFT("draft", "Draft"),
        APPROVED("approved", "Approved");

        private final String code;
        private final String title;

        OrderState(String code, String title) {
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
}
