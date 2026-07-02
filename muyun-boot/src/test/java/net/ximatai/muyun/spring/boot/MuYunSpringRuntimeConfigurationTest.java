package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuYunSpringRuntimeConfigurationTest {
    private final MuYunSpringRuntimeConfiguration configuration = new MuYunSpringRuntimeConfiguration();

    @Test
    void shouldDefaultToProductionMode() {
        MuYunSpringRuntimeProperties properties = new MuYunSpringRuntimeProperties();

        PlatformRuntimeModeProvider provider = configuration.platformRuntimeModeProvider(properties);

        assertThat(provider.currentMode()).isEqualTo(PlatformRuntimeMode.PRODUCTION);
        assertThat(provider.isProduction()).isTrue();
        assertThat(provider.isDevelopment()).isFalse();
    }

    @Test
    void shouldUseConfiguredRuntimeMode() {
        MuYunSpringRuntimeProperties properties = new MuYunSpringRuntimeProperties();
        properties.setMode(PlatformRuntimeMode.DEVELOPMENT);

        PlatformRuntimeModeProvider provider = configuration.platformRuntimeModeProvider(properties);

        assertThat(provider.currentMode()).isEqualTo(PlatformRuntimeMode.DEVELOPMENT);
        assertThat(provider.isDevelopment()).isTrue();
        assertThat(provider.isProduction()).isFalse();
    }

    @Test
    void shouldFallbackToProductionWhenConfiguredModeIsNull() {
        MuYunSpringRuntimeProperties properties = new MuYunSpringRuntimeProperties();
        properties.setMode(null);

        assertThat(configuration.platformRuntimeModeProvider(properties).currentMode())
                .isEqualTo(PlatformRuntimeMode.PRODUCTION);
    }
}
