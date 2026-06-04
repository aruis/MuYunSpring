package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.iam.StaticModuleActionRegistry;
import net.ximatai.muyun.spring.boot.iam.UserAccountStaticActions;
import net.ximatai.muyun.spring.boot.platform.StaticModuleActionDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionRegistrar;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
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
    @ConditionalOnMissingBean(StaticModuleDefinitionRegistrar.class)
    public StaticModuleDefinitionRegistrar staticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                                                          PlatformModuleActionService actionService) {
        return new StaticModuleDefinitionRegistrar(moduleService, actionService, List.of(
                new StaticModuleDefinition(
                        "iam",
                        "iam.user",
                        "用户管理",
                        null,
                        List.of(
                                StaticModuleActionDefinition.platformAction(PlatformAction.CREATE),
                                StaticModuleActionDefinition.platformAction(PlatformAction.VIEW),
                                StaticModuleActionDefinition.platformAction(PlatformAction.UPDATE),
                                StaticModuleActionDefinition.platformAction(PlatformAction.DELETE),
                                StaticModuleActionDefinition.platformAction(PlatformAction.QUERY),
                                StaticModuleActionDefinition.platformAction(PlatformAction.SORT),
                                StaticModuleActionDefinition.platformAction(PlatformAction.ENABLE),
                                StaticModuleActionDefinition.platformAction(PlatformAction.DISABLE),
                                StaticModuleActionDefinition.recordAction(
                                        UserAccountStaticActions.CHANGE_PASSWORD, "修改密码")
                        )
                )
        ));
    }
}
