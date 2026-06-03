package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ActionEndpointWebConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ActionExecutionPolicyService actionExecutionPolicyService() {
        return new AllowAllActionExecutionPolicyService();
    }

    @Bean
    public ActionEndpointContextResolver actionEndpointContextResolver() {
        return new ActionEndpointContextResolver();
    }

    @Bean
    public ActionEndpointInterceptor actionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                                              ActionEndpointContextResolver contextResolver) {
        return new ActionEndpointInterceptor(policyService, contextResolver);
    }
}

@Configuration
@ConditionalOnBean(ActionEndpointInterceptor.class)
class ActionEndpointInterceptorRegistration implements WebMvcConfigurer {
    private final ActionEndpointInterceptor interceptor;

    ActionEndpointInterceptorRegistration(ActionEndpointInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE + 200);
    }
}
