package net.ximatai.muyun.spring.boot.platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleUiViewCodesTest {
    @Test
    void shouldResolveChildResourceDefaultFormViewCode() {
        assertThat(ModuleUiViewCodes.childResourceDefaultForm("item")).isEqualTo("item_default_form");
        assertThat(ModuleUiViewCodes.childResourceDefaultForm("position"))
                .isEqualTo("position_default_form");
    }

    @Test
    void shouldRejectInvalidChildResourceCode() {
        assertThatThrownBy(() -> ModuleUiViewCodes.childResourceDefaultForm("Position"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModuleUiViewCodes.childResourceDefaultForm(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
