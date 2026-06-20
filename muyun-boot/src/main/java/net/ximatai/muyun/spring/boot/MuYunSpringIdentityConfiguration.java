package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.iam.PlatformSuperAdminAuthorizationInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.iam.PlatformSuperAdminSystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapRunner;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionScanner;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(MuYunSpringInitialAdminProperties.class)
public class MuYunSpringIdentityConfiguration {
    @Bean
    @Primary
    public ActiveTenantVerifier activeTenantVerifier(TenantService tenantService) {
        return tenantService;
    }

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
    @ConditionalOnMissingBean(SystemMenuSchemeAccessPolicy.class)
    public SystemMenuSchemeAccessPolicy systemMenuSchemeAccessPolicy() {
        return new PlatformSuperAdminSystemMenuSchemeAccessPolicy();
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
    @ConditionalOnMissingBean(InitialDataExecutor.class)
    public InitialDataExecutor initialDataExecutor(List<InitialDataAbility<?>> abilities,
                                                   List<InitialDataDeclarationProvider> providers) {
        return new InitialDataExecutor(abilities, providers);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformBootstrapRunner.class)
    public PlatformBootstrapRunner platformBootstrapRunner(List<PlatformBootstrapTask> tasks) {
        return new PlatformBootstrapRunner(tasks);
    }

    @Bean
    @ConditionalOnMissingBean(InitialDataBootstrapTask.class)
    public InitialDataBootstrapTask initialDataBootstrapTask(InitialDataExecutor initialDataExecutor) {
        return new InitialDataBootstrapTask(initialDataExecutor);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformSuperAdminAuthorizationInitialDataDeclarationProvider.class)
    public PlatformSuperAdminAuthorizationInitialDataDeclarationProvider platformSuperAdminAuthorizationInitialDataDeclarationProvider(
            RoleService roleService,
            RoleGrantDao roleGrantDao,
            RoleActionDao roleActionDao,
            PlatformModuleActionService moduleActionService) {
        return new PlatformSuperAdminAuthorizationInitialDataDeclarationProvider(
                roleService, roleGrantDao, roleActionDao, moduleActionService);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformMenuInitialDataDeclarationProvider.class)
    public PlatformMenuInitialDataDeclarationProvider platformMenuInitialDataDeclarationProvider(
            MenuService menuService,
            ApplicationContext applicationContext) {
        return new PlatformMenuInitialDataDeclarationProvider(menuService, applicationContext);
    }
}
