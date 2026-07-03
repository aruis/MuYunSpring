package net.ximatai.muyun.spring.boot;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.boot.iam.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.platform.DefaultTenantMenuProvisioner;
import net.ximatai.muyun.spring.boot.platform.DemoBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapRunner;
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
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MuYunSpringIdentityConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    public ActiveTenantVerifier activeTenantVerifier(TenantService tenantService) {
        return tenantService;
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public CurrentUserProvider currentUserProvider(ObjectProvider<UserSessionService> userSessionService) {
        UserSessionService service = userSessionService.getIfAvailable();
        return service == null ? Optional::empty : new BearerTokenCurrentUserProvider(service);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public CurrentUserWebFilter currentUserWebFilter(CurrentUserProvider currentUserProvider) {
        return new CurrentUserWebFilter(currentUserProvider);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public RequestTraceWebFilter requestTraceWebFilter() {
        return new RequestTraceWebFilter();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public SystemMenuSchemeAccessPolicy systemMenuSchemeAccessPolicy() {
        return SystemMenuSchemeAccessPolicy.DENY_ALL;
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public StaticModuleActionRegistry staticModuleActionRegistry() {
        return new StaticModuleActionRegistry();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public StaticModuleDefinitionScanner staticModuleDefinitionScanner(BeanManager beanManager) {
        return new StaticModuleDefinitionScanner(beanManager);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public StaticModuleDefinitionCatalog staticModuleDefinitionCatalog(ObjectProvider<StaticModuleDefinition> definitions,
                                                                       StaticModuleDefinitionScanner scanner) {
        return new StaticModuleDefinitionCatalog(definitions.orderedStream().toList(), List.of(scanner));
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                          PlatformModuleActionService actionService,
                                                                          StaticModuleDefinitionCatalog catalog) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, catalog, true);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public InitialDataExecutor initialDataExecutor(ObjectProvider<InitialDataAbility<?>> abilities,
                                                   ObjectProvider<InitialDataDeclarationProvider> providers) {
        return new InitialDataExecutor(abilities.orderedStream().toList(), providers.orderedStream().toList());
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public PlatformBootstrapRunner platformBootstrapRunner(ObjectProvider<PlatformBootstrapTask> tasks,
                                                           MuYunSpringPlatformBootstrapProperties properties) {
        return new PlatformBootstrapRunner(tasks.orderedStream().toList(), properties.isEnabled());
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public InitialDataBootstrapTask initialDataBootstrapTask(InitialDataExecutor initialDataExecutor) {
        return new InitialDataBootstrapTask(initialDataExecutor);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public DefaultTenantMenuProvisioner defaultTenantMenuProvisioner(MenuSchemeService menuSchemeService,
                                                                     MenuService menuService) {
        return new DefaultTenantMenuProvisioner(menuSchemeService, menuService);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
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

    @Produces
    @ApplicationScoped
    @DefaultBean
    public PlatformMenuInitialDataDeclarationProvider platformMenuInitialDataDeclarationProvider(
            MenuService menuService,
            BeanManager beanManager) {
        return new PlatformMenuInitialDataDeclarationProvider(menuService, beanManager);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public PlatformDictionaryInitialDataDeclarationProvider platformDictionaryInitialDataDeclarationProvider(
            DictionaryInitialDataDeclarations dictionaryInitialDataDeclarations) {
        return new PlatformDictionaryInitialDataDeclarationProvider(dictionaryInitialDataDeclarations);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public DictionaryInitialDataDeclarations dictionaryInitialDataDeclarations(DictionaryCategoryService categoryService,
                                                                               DictionaryItemService itemService) {
        return new DictionaryInitialDataDeclarations(categoryService, itemService);
    }
}
