package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.iam.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapRunner;
import net.ximatai.muyun.spring.boot.platform.DefaultTenantMenuProvisioner;
import net.ximatai.muyun.spring.boot.platform.DefaultTenantApplicationProvisioner;
import net.ximatai.muyun.spring.boot.platform.DefaultOrganizationRoleProvisioner;
import net.ximatai.muyun.spring.boot.platform.DefaultTenantRoleProvisioner;
import net.ximatai.muyun.spring.boot.platform.DemoBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.TenantApplicationReconciliationTask;
import net.ximatai.muyun.spring.boot.platform.PlatformDictionaryInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionScanner;
import net.ximatai.muyun.spring.boot.platform.StaticModuleReferenceCompiler;
import net.ximatai.muyun.spring.boot.platform.StaticApplicationDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticApplicationDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticApplicationDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.platform.StaticApplicationDefinitionScanner;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.boot.web.RequestTraceWebFilter;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.dynamic.metadata.StaticEntityDefinitionCompiler;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
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
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
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
    @ConditionalOnMissingBean(StaticApplicationDefinitionScanner.class)
    public StaticApplicationDefinitionScanner staticApplicationDefinitionScanner(ApplicationContext applicationContext) {
        return new StaticApplicationDefinitionScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(StaticApplicationDefinitionCatalog.class)
    public StaticApplicationDefinitionCatalog staticApplicationDefinitionCatalog(
            List<StaticApplicationDefinition> definitions,
            StaticApplicationDefinitionScanner scanner) {
        return new StaticApplicationDefinitionCatalog(definitions, List.of(scanner));
    }

    @Bean
    public StaticApplicationDefinition platformStaticApplicationDefinition() {
        return StaticApplicationDefinition.of("platform", "平台能力", 10);
    }

    @Bean
    public StaticApplicationDefinition iamStaticApplicationDefinition() {
        return StaticApplicationDefinition.of("iam", "身份权限", 20);
    }

    @Bean
    @ConditionalOnBean(ApplicationService.class)
    @ConditionalOnMissingBean(StaticApplicationDefinitionRegistrar.class)
    public StaticApplicationDefinitionRegistrar staticApplicationDefinitionRegistrar(
            ApplicationService applicationService,
            StaticApplicationDefinitionCatalog catalog) {
        return new StaticApplicationDefinitionRegistrar(applicationService, catalog);
    }

    @Bean
    public StaticModuleDefinition employeeAccountStaticModuleDefinition() {
        return StaticModuleDefinition.builder("iam", EmployeeAccountService.MODULE_ALIAS, "职员账号绑定")
                .entry(ModuleEntryType.MODULE, null, null)
                .capabilities(java.util.Set.of(EntityCapability.CRUD))
                .entities(List.of(new StaticEntityDefinitionCompiler().compile(
                        "employee_account",
                        "职员账号绑定",
                        EmployeeAccount.class
                )))
                .references(StaticModuleReferenceCompiler.compile(EmployeeAccount.class))
                .modelClass(EmployeeAccount.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    public StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                          PlatformModuleActionService actionService,
                                                                          StaticModuleDefinitionCatalog catalog,
                                                                          StaticApplicationDefinitionCatalog applicationCatalog) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, catalog, true, applicationCatalog);
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
    @ConditionalOnBean({TenantService.class, TenantApplicationService.class})
    @ConditionalOnMissingBean(TenantApplicationReconciliationTask.class)
    public TenantApplicationReconciliationTask tenantApplicationReconciliationTask(
            TenantService tenantService,
            TenantApplicationService tenantApplicationService) {
        return new TenantApplicationReconciliationTask(tenantService, tenantApplicationService);
    }

    @Bean
    @ConditionalOnBean({MenuSchemeService.class, MenuService.class})
    @ConditionalOnMissingBean(DefaultTenantMenuProvisioner.class)
    public DefaultTenantMenuProvisioner defaultTenantMenuProvisioner(MenuSchemeService menuSchemeService,
                                                                    MenuService menuService) {
        return new DefaultTenantMenuProvisioner(menuSchemeService, menuService);
    }

    @Bean
    @ConditionalOnBean(TenantApplicationService.class)
    @ConditionalOnMissingBean(DefaultTenantApplicationProvisioner.class)
    public DefaultTenantApplicationProvisioner defaultTenantApplicationProvisioner(
            TenantApplicationService tenantApplicationService) {
        return new DefaultTenantApplicationProvisioner(tenantApplicationService);
    }

    @Bean
    @ConditionalOnBean({RoleService.class, BuiltInRolePermissionTemplateService.class})
    @ConditionalOnMissingBean(DefaultTenantRoleProvisioner.class)
    public DefaultTenantRoleProvisioner defaultTenantRoleProvisioner(
            RoleService roleService,
            BuiltInRolePermissionTemplateService rolePermissionTemplateService) {
        return new DefaultTenantRoleProvisioner(roleService, rolePermissionTemplateService);
    }

    @Bean
    @ConditionalOnBean({RoleService.class, BuiltInRolePermissionTemplateService.class})
    @ConditionalOnMissingBean(DefaultOrganizationRoleProvisioner.class)
    public DefaultOrganizationRoleProvisioner defaultOrganizationRoleProvisioner(
            RoleService roleService,
            BuiltInRolePermissionTemplateService rolePermissionTemplateService) {
        return new DefaultOrganizationRoleProvisioner(roleService, rolePermissionTemplateService);
    }

    @Bean
    @ConditionalOnBean({TenantService.class, OrganizationService.class, DepartmentService.class, EmployeeService.class,
            UserAccountService.class, EmployeeAccountService.class, DefaultTenantRoleProvisioner.class})
    @ConditionalOnMissingBean(DemoBootstrapTask.class)
    public DemoBootstrapTask demoBootstrapTask(MuYunSpringDemoBootstrapProperties properties,
                                               TenantService tenantService,
                                               OrganizationService organizationService,
                                               DepartmentService departmentService,
                                               EmployeeService employeeService,
                                               UserAccountService userAccountService,
                                               EmployeeAccountService employeeAccountService,
                                               DefaultTenantRoleProvisioner tenantRoleProvisioner) {
        return new DemoBootstrapTask(properties, tenantService, organizationService, departmentService, employeeService,
                userAccountService, employeeAccountService, tenantRoleProvisioner);
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
