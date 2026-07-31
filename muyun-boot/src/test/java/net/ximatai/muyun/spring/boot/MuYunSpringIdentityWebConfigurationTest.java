package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.web.RequestTraceWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MuYunSpringIdentityWebConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MuYunSpringIdentityWebConfiguration.class);

    @Test
    void shouldProvideWebIdentityContextWithoutSessionService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CurrentUserProvider.class);
            assertThat(context).hasSingleBean(CurrentUserWebFilter.class);
            assertThat(context).hasSingleBean(RequestTraceWebFilter.class);
            assertThat(context.getBean(CurrentUserProvider.class).currentUser()).isEmpty();
        });
    }

    @Test
    void shouldBackOffWhenApplicationProvidesCurrentUserProvider() {
        CurrentUserProvider customProvider = Optional::empty;

        contextRunner.withBean(CurrentUserProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context.getBean(CurrentUserProvider.class)).isSameAs(customProvider);
                    assertThat(context).hasSingleBean(CurrentUserWebFilter.class);
                    assertThat(context).hasSingleBean(RequestTraceWebFilter.class);
                });
    }
}
