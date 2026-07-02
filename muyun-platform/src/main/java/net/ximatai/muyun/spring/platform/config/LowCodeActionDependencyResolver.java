package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LowCodeActionDependencyResolver implements LowCodePackageDependencyResolver {
    private final PlatformModuleActionService actionService;

    public LowCodeActionDependencyResolver(PlatformModuleActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.ACTION;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        return actionService.findByModuleAliasAndActionCode(dependency.moduleAlias(), dependency.alias()) != null;
    }
}
