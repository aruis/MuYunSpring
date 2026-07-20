package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.identity.CurrentUserTimeZoneResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserSessionCollaboratorConfiguration {
    @Bean
    UserSessionCollaborators userSessionCollaborators(
            ObjectProvider<UserSessionRevocationService> revocationService,
            ObjectProvider<UserSecurityEventPublisher> securityEventPublisher,
            ObjectProvider<CurrentUserTimeZoneResolver> timeZoneResolver,
            ObjectProvider<UserSessionPresenceLookup> presenceLookup,
            ApplicationEventPublisher applicationEventPublisher) {
        return new UserSessionCollaborators(
                revocationService::getIfAvailable,
                () -> securityEventPublisher.getIfAvailable(() -> UserSecurityEventPublisher.NOOP),
                () -> event -> applicationEventPublisher.publishEvent(event),
                timeZoneResolver.getIfAvailable(() -> CurrentUserTimeZoneResolver.NONE),
                () -> presenceLookup.getIfAvailable(() -> UserSessionPresenceLookup.NONE));
    }
}
