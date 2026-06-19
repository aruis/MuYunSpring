package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
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

            assertThat(abilities.values())
                    .anyMatch(MenuSchemeService.class::isInstance)
                    .anyMatch(MenuService.class::isInstance);
            assertThat(context).hasSingleBean(InitialDataExecutor.class);
            assertThat(context).hasSingleBean(PlatformMenuInitialDataDeclarationProvider.class);
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

        @SuppressWarnings("unchecked")
        private BaseDao<MenuScheme, String> menuSchemeDao() {
            return mock(BaseDao.class);
        }

        @SuppressWarnings("unchecked")
        private BaseDao<Menu, String> menuDao() {
            return mock(BaseDao.class);
        }
    }
}
