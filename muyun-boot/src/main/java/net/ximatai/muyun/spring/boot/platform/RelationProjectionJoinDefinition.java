package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.List;

public record RelationProjectionJoinDefinition(String relationCode,
                                               EntityDefinition targetEntity,
                                               RelationProjectionCardinality cardinality,
                                               List<RelationProjectionJoinStep> steps) {
    public RelationProjectionJoinDefinition {
        relationCode = PlatformNameRules.requireIdentifier(relationCode, "relationCode");
        if (targetEntity == null) {
            throw new IllegalArgumentException("projection join target entity must not be null");
        }
        if (cardinality == null) {
            throw new IllegalArgumentException("projection join cardinality must not be null: " + relationCode);
        }
        if (!relationCode.equals(targetEntity.alias())) {
            throw new IllegalArgumentException("projection join target entity alias must match relation code: "
                    + relationCode + " != " + targetEntity.alias());
        }
        steps = steps == null ? List.of() : List.copyOf(steps);
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("projection join steps must not be empty: " + relationCode);
        }
    }
}
