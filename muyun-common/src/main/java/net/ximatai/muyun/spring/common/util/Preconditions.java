package net.ximatai.muyun.spring.common.util;

public final class Preconditions {
    private Preconditions() {
    }

    public static String requireText(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        String text = value.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
