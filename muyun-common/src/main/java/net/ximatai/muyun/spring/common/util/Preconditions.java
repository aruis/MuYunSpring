package net.ximatai.muyun.spring.common.util;

import java.util.Objects;

public final class Preconditions {
    private Preconditions() {
    }

    public static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
