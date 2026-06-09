package net.ximatai.muyun.spring.common.web;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformWebPathRulesTest {
    @Test
    void shouldReserveAllPlatformActionCodes() {
        assertThat(PlatformWebPathRules.reservedWebActionCodes())
                .containsAll(Arrays.stream(PlatformAction.values())
                        .map(PlatformAction::code)
                        .toList());
    }

    @Test
    void shouldReserveStandardDynamicWebRoots() {
        assertThat(PlatformWebPathRules.reservedWebActionCodes())
                .contains("actions", "describe", "entities", "exchange", "openapi", "references");
    }
}
