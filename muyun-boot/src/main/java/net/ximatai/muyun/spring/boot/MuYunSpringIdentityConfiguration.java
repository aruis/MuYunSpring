package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class MuYunSpringIdentityConfiguration {
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider currentUserProvider() {
        return Optional::empty;
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserWebFilter.class)
    public CurrentUserWebFilter currentUserWebFilter(CurrentUserProvider currentUserProvider) {
        return new CurrentUserWebFilter(currentUserProvider);
    }
}
