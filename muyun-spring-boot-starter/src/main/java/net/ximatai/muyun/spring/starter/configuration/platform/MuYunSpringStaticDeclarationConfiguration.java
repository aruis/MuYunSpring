package net.ximatai.muyun.spring.starter.configuration.platform;

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

/**
 * 静态应用与模块声明装配：扫描 Java 注解、汇入目录，并在启动写入前完成预检。
 * 这里仅编译声明事实，不创建 HTTP 端点，也不替代领域 Service。
 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringStaticDeclarationConfiguration {
    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionScanner.class)
    /** 从 Spring 上下文发现静态模块声明与 Service 能力组合。 */
    StaticModuleDefinitionScanner staticModuleDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticModuleDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionCatalog.class)
    /** 合并显式定义和扫描结果，形成可供注册任务消费的模块目录。 */
    StaticModuleDefinitionCatalog staticModuleDefinitionCatalog(List<StaticModuleDefinition> definitions,
                                                                 StaticModuleDefinitionScanner scanner) {
        return new StaticModuleDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(StaticApplicationDefinitionScanner.class)
    /** 从 Spring 上下文发现静态应用身份声明。 */
    StaticApplicationDefinitionScanner staticApplicationDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticApplicationDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticApplicationDefinitionCatalog.class)
    /** 合并显式定义和扫描结果，形成应用目录。 */
    StaticApplicationDefinitionCatalog staticApplicationDefinitionCatalog(
            List<StaticApplicationDefinition> definitions,
            StaticApplicationDefinitionScanner scanner) {
        return new StaticApplicationDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(StaticDeclarationPreflightTask.class)
    /** 在写入前校验应用、模块别名及归属关系，尽早阻断无效静态声明。 */
    StaticDeclarationPreflightTask staticDeclarationPreflightTask(
            StaticApplicationDefinitionCatalog applicationCatalog,
            StaticModuleDefinitionCatalog moduleCatalog) {
        return new StaticDeclarationPreflightTask(applicationCatalog, moduleCatalog);
    }

    @Bean
    @ConditionalOnBean(ApplicationService.class)
    @ConditionalOnMissingBean(StaticApplicationDefinitionRegistrar.class)
    /** 应用领域可用时，将静态应用身份同步为平台托管应用记录。 */
    StaticApplicationDefinitionRegistrar staticApplicationDefinitionRegistrar(
            ApplicationService applicationService,
            StaticApplicationDefinitionCatalog catalog) {
        return new StaticApplicationDefinitionRegistrar(applicationService, catalog);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    /** 将通过预检的模块、动作和读投影声明同步为平台模块记录。 */
    StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                    PlatformModuleActionService actionService,
                                                                    StaticModuleDefinitionCatalog catalog,
                                                                    StaticApplicationDefinitionCatalog applicationCatalog) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, catalog, true, applicationCatalog);
    }
}
