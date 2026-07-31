package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.iam.web.security.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.web.RequestTraceWebFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/** Assembles HTTP identity context propagation for a Spring MVC application. */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringIdentityWebConfiguration {
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider currentUserProvider(ObjectProvider<UserSessionService> userSessionService) {
        UserSessionService service = userSessionService.getIfAvailable();
        return service == null ? Optional::empty : new BearerTokenCurrentUserProvider(service);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserWebFilter.class)
    CurrentUserWebFilter currentUserWebFilter(CurrentUserProvider currentUserProvider) {
        return new CurrentUserWebFilter(currentUserProvider);
    }

    @Bean
    @ConditionalOnMissingBean(RequestTraceWebFilter.class)
    RequestTraceWebFilter requestTraceWebFilter() {
        return new RequestTraceWebFilter();
    }
}
