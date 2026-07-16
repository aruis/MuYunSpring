package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.ability.action.ActionMessageReporter;
import net.ximatai.muyun.spring.ability.action.DataChangeModuleAliasResolver;
import net.ximatai.muyun.spring.ability.action.DataChangeRecorder;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.springframework.beans.factory.ObjectProvider;
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
    public ActionEndpointContextResolver actionEndpointContextResolver(
            ObjectProvider<PlatformModuleActionService> moduleActionService) {
        return new ActionEndpointContextResolver(moduleActionService.getIfAvailable());
    }

    @Bean
    public ActionEndpointInterceptor actionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                                              ActionEndpointContextResolver contextResolver,
                                                              ObjectProvider<EmployeeDelegationService>
                                                                      employeeDelegationService) {
        EmployeeDelegationService delegationService = employeeDelegationService.getIfAvailable();
        return new ActionEndpointInterceptor(policyService, contextResolver,
                delegationService == null ? null : new ActingRequestResolver(delegationService));
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessMutationInterceptor businessMutationInterceptor() {
        return new BusinessMutationInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataChangeRecorder dataChangeRecorder() {
        return new DataChangeRecorder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActionMessageReporter actionMessageReporter() {
        return new ActionMessageReporter();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataChangeModuleAliasResolver dataChangeModuleAliasResolver(
            StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        return new StaticModuleDataChangeAliasResolver(staticModuleDefinitionCatalog);
    }

    @Bean
    @ConditionalOnBean(ActionEndpointInterceptor.class)
    public WebMvcConfigurer actionEndpointInterceptorRegistration(
            ActionEndpointInterceptor actionEndpointInterceptor,
            ObjectProvider<BusinessMutationInterceptor> businessMutationInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(actionEndpointInterceptor)
                        .addPathPatterns("/**")
                        .order(Ordered.HIGHEST_PRECEDENCE + 200);
                BusinessMutationInterceptor mutationInterceptor = businessMutationInterceptor.getIfAvailable();
                if (mutationInterceptor != null) {
                    registry.addInterceptor(mutationInterceptor)
                            .addPathPatterns("/**")
                            .order(Ordered.HIGHEST_PRECEDENCE + 210);
                }
            }
        };
    }
}
