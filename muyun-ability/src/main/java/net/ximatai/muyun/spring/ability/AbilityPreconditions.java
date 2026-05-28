package net.ximatai.muyun.spring.ability;

import java.util.Objects;

public final class AbilityPreconditions {
    private AbilityPreconditions() {
    }

    public static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
