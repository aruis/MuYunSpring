package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.List;

public record StaticProjectionJoinDefinition(String relationCode,
                                             EntityDefinition targetEntity,
                                             List<StaticProjectionJoinStep> steps) {
    public StaticProjectionJoinDefinition {
        relationCode = PlatformNameRules.requireIdentifier(relationCode, "relationCode");
        if (targetEntity == null) {
            throw new IllegalArgumentException("projection join target entity must not be null");
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
