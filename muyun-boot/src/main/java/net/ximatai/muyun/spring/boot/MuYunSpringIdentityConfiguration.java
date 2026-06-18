package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.platform.InitialDataApplicationRunner;
import net.ximatai.muyun.spring.boot.platform.PlatformAdminMenuInitialDataContribution;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionScanner;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContribution;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
public class MuYunSpringIdentityConfiguration {
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider currentUserProvider(ObjectProvider<UserSessionService> userSessionService) {
        UserSessionService service = userSessionService.getIfAvailable();
        return service == null ? Optional::empty : new BearerTokenCurrentUserProvider(service);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserWebFilter.class)
    public CurrentUserWebFilter currentUserWebFilter(CurrentUserProvider currentUserProvider) {
        return new CurrentUserWebFilter(currentUserProvider);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleActionRegistry.class)
    public StaticModuleActionRegistry staticModuleActionRegistry() {
        return new StaticModuleActionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionScanner.class)
    public StaticModuleDefinitionScanner staticModuleDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticModuleDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    public StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                          PlatformModuleActionService actionService,
                                                                          StaticModuleDefinitionScanner scanner) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, List.of(), List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(InitialDataAbility.class)
    public InitialDataAbility initialDataAbility(List<InitialDataContribution> contributions) {
        return new InitialDataAbility(contributions);
    }

    @Bean
    @ConditionalOnMissingBean(InitialDataApplicationRunner.class)
    public InitialDataApplicationRunner initialDataApplicationRunner(InitialDataAbility initialDataAbility) {
        return new InitialDataApplicationRunner(initialDataAbility);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformAdminMenuInitialDataContribution.class)
    public PlatformAdminMenuInitialDataContribution platformAdminMenuInitialDataContribution(
            MenuSchemeService schemeService,
            MenuService menuService) {
        return new PlatformAdminMenuInitialDataContribution(schemeService, menuService);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformMenuRegistrar.class)
    public PlatformMenuRegistrar platformMenuRegistrar(MenuService menuService,
                                                       ApplicationContext applicationContext) {
        return new PlatformMenuRegistrar(menuService, applicationContext);
    }
}
