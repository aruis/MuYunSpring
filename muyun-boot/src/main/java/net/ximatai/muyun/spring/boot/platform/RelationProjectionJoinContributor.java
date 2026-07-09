package net.ximatai.muyun.spring.boot.platform;

import java.util.List;

/**
 * Compatibility escape hatch for static modules that cannot be described by
 * {@code @ModuleReference} and service-level read projections yet.
 */
@Deprecated(since = "0.1", forRemoval = false)
public interface RelationProjectionJoinContributor {
    List<RelationProjectionJoinDefinition> projectionJoins();
}
