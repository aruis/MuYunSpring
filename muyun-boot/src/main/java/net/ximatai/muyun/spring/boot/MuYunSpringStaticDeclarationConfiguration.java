package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.platform.application.ApplicationService;
import net.ximatai.muyun.spring.platform.application.StaticApplicationDefinition;
import net.ximatai.muyun.spring.platform.application.StaticApplicationDefinitionCatalog;
import net.ximatai.muyun.spring.platform.application.StaticApplicationDefinitionRegistrar;
import net.ximatai.muyun.spring.platform.application.StaticApplicationDefinitionScanner;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.module.StaticDeclarationPreflightTask;
import net.ximatai.muyun.spring.platform.module.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.platform.web.StaticModuleDefinition;
import net.ximatai.muyun.spring.platform.web.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.platform.web.StaticModuleDefinitionScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Assembles static application and module declarations before platform bootstrap writes. */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringStaticDeclarationConfiguration {
    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionScanner.class)
    StaticModuleDefinitionScanner staticModuleDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticModuleDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionCatalog.class)
    StaticModuleDefinitionCatalog staticModuleDefinitionCatalog(List<StaticModuleDefinition> definitions,
                                                                 StaticModuleDefinitionScanner scanner) {
        return new StaticModuleDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(StaticApplicationDefinitionScanner.class)
    StaticApplicationDefinitionScanner staticApplicationDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticApplicationDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticApplicationDefinitionCatalog.class)
    StaticApplicationDefinitionCatalog staticApplicationDefinitionCatalog(
            List<StaticApplicationDefinition> definitions,
            StaticApplicationDefinitionScanner scanner) {
        return new StaticApplicationDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(StaticDeclarationPreflightTask.class)
    StaticDeclarationPreflightTask staticDeclarationPreflightTask(
            StaticApplicationDefinitionCatalog applicationCatalog,
            StaticModuleDefinitionCatalog moduleCatalog) {
        return new StaticDeclarationPreflightTask(applicationCatalog, moduleCatalog);
    }

    @Bean
    @ConditionalOnBean(ApplicationService.class)
    @ConditionalOnMissingBean(StaticApplicationDefinitionRegistrar.class)
    StaticApplicationDefinitionRegistrar staticApplicationDefinitionRegistrar(
            ApplicationService applicationService,
            StaticApplicationDefinitionCatalog catalog) {
        return new StaticApplicationDefinitionRegistrar(applicationService, catalog);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                    PlatformModuleActionService actionService,
                                                                    StaticModuleDefinitionCatalog catalog,
                                                                    StaticApplicationDefinitionCatalog applicationCatalog) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, catalog, true, applicationCatalog);
    }
}
