package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.util.Objects;

/** Source-neutral semantic definition of one standard platform operation. */
public record PlatformOperationDefinition(String abilityCode,
                                          String operationCode,
                                          PlatformAction action) {
    public PlatformOperationDefinition {
        abilityCode = requireText(abilityCode, "abilityCode");
        operationCode = requireText(operationCode, "operationCode");
        action = Objects.requireNonNull(action, "action must not be null");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
