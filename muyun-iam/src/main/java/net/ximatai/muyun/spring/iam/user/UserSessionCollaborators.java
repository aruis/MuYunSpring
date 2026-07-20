package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.identity.CurrentUserTimeZoneResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public final class UserSessionCollaborators {
    private final Supplier<UserSessionRevocationService> revocationService;
    private final Supplier<UserSecurityEventPublisher> securityEventPublisher;
    private final Supplier<UserSessionLifecycleEventPublisher> lifecycleEventPublisher;
    private final CurrentUserTimeZoneResolver timeZoneResolver;
    private final Supplier<UserSessionPresenceLookup> presenceLookup;

    public UserSessionCollaborators(
            ObjectProvider<UserSessionRevocationService> revocationService,
            ObjectProvider<UserSecurityEventPublisher> securityEventPublisher,
            ObjectProvider<CurrentUserTimeZoneResolver> timeZoneResolver,
            ObjectProvider<UserSessionPresenceLookup> presenceLookup,
            ApplicationEventPublisher applicationEventPublisher) {
        this.revocationService = () -> available(revocationService);
        this.securityEventPublisher = () -> availableOrDefault(
                securityEventPublisher, UserSecurityEventPublisher.NOOP);
        this.lifecycleEventPublisher = () -> event -> applicationEventPublisher.publishEvent(event);
        this.timeZoneResolver = availableOrDefault(timeZoneResolver, CurrentUserTimeZoneResolver.NONE);
        this.presenceLookup = () -> availableOrDefault(presenceLookup, UserSessionPresenceLookup.NONE);
    }

    Supplier<UserSessionRevocationService> revocationService() {
        return revocationService;
    }

    Supplier<UserSecurityEventPublisher> securityEventPublisher() {
        return securityEventPublisher;
    }

    Supplier<UserSessionLifecycleEventPublisher> lifecycleEventPublisher() {
        return lifecycleEventPublisher;
    }

    CurrentUserTimeZoneResolver timeZoneResolver() {
        return timeZoneResolver;
    }

    Supplier<UserSessionPresenceLookup> presenceLookup() {
        return presenceLookup;
    }

    private static <T> T available(ObjectProvider<T> provider) {
        return provider == null ? null : provider.getIfAvailable();
    }

    private static <T> T availableOrDefault(ObjectProvider<T> provider, T fallback) {
        T value = available(provider);
        return value == null ? fallback : value;
    }
}
