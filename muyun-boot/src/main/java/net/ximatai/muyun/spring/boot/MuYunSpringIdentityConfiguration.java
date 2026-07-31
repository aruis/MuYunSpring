package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.iam.role.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.iam.role.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.platform.menu.DefaultTenantMenuProvisioner;
import net.ximatai.muyun.spring.iam.tenant.DefaultTenantApplicationProvisioner;
import net.ximatai.muyun.spring.iam.role.DefaultOrganizationRoleProvisioner;
import net.ximatai.muyun.spring.iam.role.DefaultTenantRoleProvisioner;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationReconciliationTask;
import net.ximatai.muyun.spring.platform.dictionary.PlatformDictionaryInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.web.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import net.ximatai.muyun.spring.iam.role.RoleService;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@EnableConfigurationProperties(MuYunSpringInitialAdminProperties.class)
@Import({MuYunSpringStaticDeclarationConfiguration.class, MuYunSpringIdentityWebConfiguration.class})
public class MuYunSpringIdentityConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean(value = ActiveTenantVerifier.class,
            ignored = {TenantService.class, TenantActiveScopedAbility.class})
    public ActiveTenantVerifier activeTenantVerifier(TenantService tenantService) {
        return tenantService;
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
