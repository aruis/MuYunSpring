package net.ximatai.muyun.spring.boot.web;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;

@ApplicationScoped
public class ActionEndpointWebConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    public ActionExecutionPolicyService actionExecutionPolicyService() {
        return new AllowAllActionExecutionPolicyService();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public ActionEndpointContextResolver actionEndpointContextResolver(
            ObjectProvider<PlatformModuleActionService> moduleActionService) {
        return new ActionEndpointContextResolver(moduleActionService.getIfAvailable());
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    public ActionEndpointInterceptor actionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                                              ActionEndpointContextResolver contextResolver,
                                                              ObjectProvider<EmployeeDelegationService>
                                                                      employeeDelegationService) {
        EmployeeDelegationService delegationService = employeeDelegationService.getIfAvailable();
        return new ActionEndpointInterceptor(policyService, contextResolver,
                delegationService == null ? null : new ActingRequestResolver(delegationService));
    }
}
