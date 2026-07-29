package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticApplicationDefinitionRegistrarTest {
    @Test
    void shouldScanAndRegisterPlatformManagedApplicationBeforeModules() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EducationConfiguration.class)) {
            StaticApplicationDefinitionCatalog catalog = new StaticApplicationDefinitionCatalog(
                    List.of(), List.of(new StaticApplicationDefinitionScanner(context)));
            ApplicationService service = mock(ApplicationService.class);
            when(service.select("education")).thenReturn(null);

            StaticApplicationDefinitionRegistrar registrar = new StaticApplicationDefinitionRegistrar(service, catalog);
            registrar.registerAll();

            var captor = forClass(Application.class);
            verify(service).insert(captor.capture());
            assertThat(captor.getValue()).satisfies(application -> {
                assertThat(application.getAlias()).isEqualTo("education");
                assertThat(application.getTitle()).isEqualTo("教学管理");
                assertThat(application.getSortOrder()).isEqualTo(100);
                assertThat(application.getEnabled()).isTrue();
                assertThat(application.getSystemManaged()).isTrue();
            });
            assertThat(registrar.order()).isLessThan(new StaticModuleDefinitionRegistrar(
                    mock(net.ximatai.muyun.spring.platform.module.PlatformModuleService.class),
                    mock(net.ximatai.muyun.spring.platform.module.PlatformModuleActionService.class),
                    List.of()).order());
        }
    }

    @Test
    void shouldRejectManualApplicationWithSameAlias() {
        ApplicationService service = mock(ApplicationService.class);
        Application manual = new Application();
        manual.setAlias("education");
        manual.setSystemManaged(Boolean.FALSE);
        when(service.select("education")).thenReturn(manual);
        StaticApplicationDefinitionRegistrar registrar = new StaticApplicationDefinitionRegistrar(service,
                List.of(StaticApplicationDefinition.of("education", "教学管理", 100)));

        assertThatThrownBy(registrar::registerAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-platform-managed application: education");
    }

    @Test
    void shouldDisableStalePlatformManagedApplicationWhenScanningDeclarations() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EducationConfiguration.class)) {
            StaticApplicationDefinitionCatalog catalog = new StaticApplicationDefinitionCatalog(
                    List.of(), List.of(new StaticApplicationDefinitionScanner(context)));
            ApplicationService service = mock(ApplicationService.class);
            when(service.select("education")).thenReturn(null);
            Application stale = new Application();
            stale.setAlias("retired");
            stale.setSystemManaged(Boolean.TRUE);
            when(service.list(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(stale));

            new StaticApplicationDefinitionRegistrar(service, catalog).registerAll();

            verify(service).disable("retired");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @PlatformStaticApplication(alias = "education", title = "教学管理")
    static class EducationConfiguration {
    }
}
