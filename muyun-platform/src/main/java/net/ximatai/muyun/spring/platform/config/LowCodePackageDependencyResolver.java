package net.ximatai.muyun.spring.platform.config;

public interface LowCodePackageDependencyResolver {
    boolean supports(LowCodePackageDependencyType type);

    boolean exists(LowCodePackageDependency dependency);
}
