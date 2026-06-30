package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformDictionaryInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MuYunSpringIdentityConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MuYunSpringIdentityConfiguration.class, MenuInitialDataAbilityConfiguration.class);

    @Test
    void shouldCollectMenuServicesAsInitialDataAbilities() {
        contextRunner.run(context -> {
            Map<String, InitialDataAbility> abilities = context.getBeansOfType(InitialDataAbility.class);
            Map<String, PlatformBootstrapTask> bootstrapTasks = context.getBeansOfType(PlatformBootstrapTask.class);

            assertThat(abilities.values())
                    .anyMatch(MenuSchemeService.class::isInstance)
                    .anyMatch(MenuService.class::isInstance);
            assertThat(context).hasSingleBean(InitialDataExecutor.class);
            assertThat(context).hasSingleBean(PlatformMenuInitialDataDeclarationProvider.class);
            assertThat(context).hasSingleBean(DictionaryInitialDataDeclarations.class);
            assertThat(context).hasSingleBean(PlatformDictionaryInitialDataDeclarationProvider.class);
            assertThat(bootstrapTasks.values())
                    .anyMatch(StaticModuleDefinitionRegistrar.class::isInstance)
                    .anyMatch(InitialDataBootstrapTask.class::isInstance);
        });
    }

    @Test
    void shouldUseTenantServiceAsActiveTenantVerifierWithoutExtraPrimaryAdapter() {
        contextRunner.run(context -> {
            Map<String, ActiveTenantVerifier> verifiers = context.getBeansOfType(ActiveTenantVerifier.class);

            assertThat(verifiers).containsKey("tenantService");
            assertThat(verifiers).containsKey("activeTenantVerifier");
            assertThat(context.getBean(ActiveTenantVerifier.class)).isSameAs(verifiers.get("activeTenantVerifier"));
        });
    }

    @Test
    void shouldBackOffDefaultActiveTenantVerifierWhenApplicationProvidesOne() {
        ActiveTenantVerifier customVerifier = tenantId -> {
        };

        contextRunner.withBean("customActiveTenantVerifier", ActiveTenantVerifier.class, () -> customVerifier,
                        beanDefinition -> beanDefinition.setPrimary(true))
                .run(context -> {
                    Map<String, ActiveTenantVerifier> verifiers = context.getBeansOfType(ActiveTenantVerifier.class);

                    assertThat(verifiers).containsEntry("customActiveTenantVerifier", customVerifier);
                    assertThat(verifiers).doesNotContainKey("activeTenantVerifier");
                    assertThat(context.getBean(ActiveTenantVerifier.class)).isSameAs(customVerifier);
                });
    }

    @Test
    void shouldLimitSystemMenuFallbackToPlatformSuperAdminTenant() {
        contextRunner.run(context -> {
            SystemMenuSchemeAccessPolicy policy = context.getBean(SystemMenuSchemeAccessPolicy.class);

            assertThat(policy.canUseSystemMenuScheme(CurrentUser.tenantUser(
                    UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID,
                    UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                    TenantService.PLATFORM_TENANT_ID))).isTrue();
            assertThat(policy.canUseSystemMenuScheme(CurrentUser.tenantUser(
                    UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID,
                    UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                    "tenant-a"))).isFalse();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MenuInitialDataAbilityConfiguration {
        @Bean
        MenuSchemeService menuSchemeService() {
            return new MenuSchemeService(menuSchemeDao());
        }

        @Bean
        MenuService menuService(MenuSchemeService menuSchemeService, PlatformModuleService platformModuleService) {
            return new MenuService(menuDao(), menuSchemeService, platformModuleService, Optional.empty());
        }

        @Bean
        PlatformModuleService platformModuleService() {
            return mock(PlatformModuleService.class);
        }

        @Bean
        PlatformModuleActionService platformModuleActionService() {
            return mock(PlatformModuleActionService.class);
        }

        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean
        RoleGrantDao roleGrantDao() {
            return mock(RoleGrantDao.class);
        }

        @Bean
        RoleActionDao roleActionDao() {
            return mock(RoleActionDao.class);
        }

        @Bean
        TenantService tenantService() {
            return mock(TenantService.class);
        }

        @Bean
        DictionaryCategoryService dictionaryCategoryService() {
            return new DictionaryCategoryService(dictionaryCategoryDao());
        }

        @Bean
        DictionaryItemService dictionaryItemService(DictionaryCategoryService dictionaryCategoryService) {
            return new DictionaryItemService(dictionaryItemDao(), dictionaryCategoryService);
        }

        @SuppressWarnings("unchecked")
        private BaseDao<MenuScheme, String> menuSchemeDao() {
            return mock(BaseDao.class);
        }

        @SuppressWarnings("unchecked")
        private BaseDao<Menu, String> menuDao() {
            return mock(BaseDao.class);
        }

        @SuppressWarnings("unchecked")
        private BaseDao<DictionaryCategory, String> dictionaryCategoryDao() {
            return mock(BaseDao.class);
        }

        @SuppressWarnings("unchecked")
        private BaseDao<DictionaryItem, String> dictionaryItemDao() {
            return mock(BaseDao.class);
        }
    }
}
