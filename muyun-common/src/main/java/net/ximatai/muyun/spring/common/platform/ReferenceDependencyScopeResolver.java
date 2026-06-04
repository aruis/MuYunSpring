package net.ximatai.muyun.spring.common.platform;

import java.util.Optional;

public interface ReferenceDependencyScopeResolver {
    Optional<ReferenceDependencyScopePlan> resolve(ReferenceDependencyScopeRequest request);
}
