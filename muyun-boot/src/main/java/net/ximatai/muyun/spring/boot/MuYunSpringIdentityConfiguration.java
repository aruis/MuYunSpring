package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.iam.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapRunner;
import net.ximatai.muyun.spring.boot.platform.DefaultTenantMenuProvisioner;
import net.ximatai.muyun.spring.boot.platform.DemoBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformDictionaryInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionScanner;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.boot.web.RequestTraceWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties({MuYunSpringInitialAdminProperties.class, MuYunSpringDemoBootstrapProperties.class})
public class MuYunSpringIdentityConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean(value = ActiveTenantVerifier.class,
            ignored = {TenantService.class, TenantActiveScopedAbility.class})
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
    @ConditionalOnMissingBean(RequestTraceWebFilter.class)
    public RequestTraceWebFilter requestTraceWebFilter() {
        return new RequestTraceWebFilter();
    }

    @Bean
    @ConditionalOnMissingBean(SystemMenuSchemeAccessPolicy.class)
    public SystemMenuSchemeAccessPolicy systemMenuSchemeAccessPolicy() {
        return SystemMenuSchemeAccessPolicy.DENY_ALL;
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
    @ConditionalOnMissingBean(StaticModuleDefinitionCatalog.class)
    public StaticModuleDefinitionCatalog staticModuleDefinitionCatalog(List<StaticModuleDefinition> definitions,
                                                                       StaticModuleDefinitionScanner scanner) {
        return new StaticModuleDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    public StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                          PlatformModuleActionService actionService,
                                                                          StaticModuleDefinitionCatalog catalog) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, catalog, true);
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
    @ConditionalOnBean({MenuSchemeService.class, MenuService.class})
    @ConditionalOnMissingBean(DefaultTenantMenuProvisioner.class)
    public DefaultTenantMenuProvisioner defaultTenantMenuProvisioner(MenuSchemeService menuSchemeService,
                                                                    MenuService menuService) {
        return new DefaultTenantMenuProvisioner(menuSchemeService, menuService);
    }

    @Bean
    @ConditionalOnBean({TenantService.class, OrganizationService.class, DepartmentService.class, EmployeeService.class,
            UserAccountService.class, EmployeeAccountService.class, RoleService.class,
            BuiltInRolePermissionTemplateService.class})
    @ConditionalOnMissingBean(DemoBootstrapTask.class)
    public DemoBootstrapTask demoBootstrapTask(MuYunSpringDemoBootstrapProperties properties,
                                               TenantService tenantService,
                                               OrganizationService organizationService,
                                               DepartmentService departmentService,
                                               EmployeeService employeeService,
                                               UserAccountService userAccountService,
                                               EmployeeAccountService employeeAccountService,
                                               RoleService roleService,
                                               BuiltInRolePermissionTemplateService rolePermissionTemplateService) {
        return new DemoBootstrapTask(properties, tenantService, organizationService, departmentService, employeeService,
                userAccountService, employeeAccountService, roleService, rolePermissionTemplateService);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformMenuInitialDataDeclarationProvider.class)
    public PlatformMenuInitialDataDeclarationProvider platformMenuInitialDataDeclarationProvider(
            MenuService menuService,
            ApplicationContext applicationContext) {
        return new PlatformMenuInitialDataDeclarationProvider(menuService, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformDictionaryInitialDataDeclarationProvider.class)
    public PlatformDictionaryInitialDataDeclarationProvider platformDictionaryInitialDataDeclarationProvider(
            DictionaryInitialDataDeclarations dictionaryInitialDataDeclarations) {
        return new PlatformDictionaryInitialDataDeclarationProvider(dictionaryInitialDataDeclarations);
    }

    @Bean
    @ConditionalOnMissingBean(DictionaryInitialDataDeclarations.class)
    public DictionaryInitialDataDeclarations dictionaryInitialDataDeclarations(DictionaryCategoryService categoryService,
                                                                               DictionaryItemService itemService) {
        return new DictionaryInitialDataDeclarations(categoryService, itemService);
    }
}
