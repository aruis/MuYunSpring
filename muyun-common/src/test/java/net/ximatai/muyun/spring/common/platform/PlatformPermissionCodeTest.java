package net.ximatai.muyun.spring.common.platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformPermissionCodeTest {
    @Test
    void shouldBuildStableActionPermissionCode() {
        assertThat(PlatformPermissionCode.action("sales.contract", "submit"))
                .isEqualTo("sales.contract:submit");
    }

    @Test
    void shouldRejectBlankParts() {
        assertThatThrownBy(() -> PlatformPermissionCode.action(" ", "submit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
        assertThatThrownBy(() -> PlatformPermissionCode.action("sales.contract", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actionCode");
    }
}
