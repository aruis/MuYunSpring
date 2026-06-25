package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MuYunSpringRuntimeConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MuYunSpringRuntimeConfiguration.class);

    @Test
    void shouldDefaultToProductionMode() {
        contextRunner.run(context -> {
            PlatformRuntimeModeProvider provider = context.getBean(PlatformRuntimeModeProvider.class);

            assertThat(provider.currentMode()).isEqualTo(PlatformRuntimeMode.PRODUCTION);
            assertThat(provider.isProduction()).isTrue();
            assertThat(provider.isDevelopment()).isFalse();
        });
    }

    @Test
    void shouldBindDevelopmentModeFromConfiguration() {
        contextRunner.withPropertyValues("muyun.runtime.mode=development")
                .run(context -> {
                    PlatformRuntimeModeProvider provider = context.getBean(PlatformRuntimeModeProvider.class);

                    assertThat(provider.currentMode()).isEqualTo(PlatformRuntimeMode.DEVELOPMENT);
                    assertThat(provider.isDevelopment()).isTrue();
                    assertThat(provider.isProduction()).isFalse();
                });
    }

    @Test
    void shouldBindProductionModeFromConfiguration() {
        contextRunner.withPropertyValues("muyun.runtime.mode=PRODUCTION")
                .run(context -> assertThat(context.getBean(PlatformRuntimeModeProvider.class).currentMode())
                        .isEqualTo(PlatformRuntimeMode.PRODUCTION));
    }

    @Test
    void shouldRespectCustomRuntimeModeProvider() {
        PlatformRuntimeModeProvider customProvider = () -> PlatformRuntimeMode.DEVELOPMENT;

        contextRunner.withBean(PlatformRuntimeModeProvider.class, () -> customProvider)
                .run(context -> assertThat(context.getBean(PlatformRuntimeModeProvider.class))
                        .isSameAs(customProvider));
    }
}
