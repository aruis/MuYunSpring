package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LowCodeModuleDependencyResolver implements LowCodePackageDependencyResolver {
    private final PlatformModuleService moduleService;

    public LowCodeModuleDependencyResolver(PlatformModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.MODULE;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        return moduleService.resolveVisibleModule(dependency.moduleAlias()) != null;
    }
}
