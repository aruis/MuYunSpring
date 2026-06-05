package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

final class DefaultFieldMasker implements FieldMasker {
    @Override
    public Object mask(String fieldName, Object value, FieldMaskingPolicy policy, FieldOutputContext context) {
        if (value == null || policy == null || !policy.enabled()) {
            return value;
        }
        String text = String.valueOf(value);
        return switch (policy) {
            case FULL -> repeat('*', Math.max(1, text.length()));
            case PHONE -> maskPhone(text);
            case EMAIL -> maskEmail(text);
            case MIDDLE -> maskMiddle(text);
            case NONE -> text;
        };
    }

    private String maskPhone(String text) {
        if (text.length() <= 7) {
            return maskMiddle(text);
        }
        return text.substring(0, 3) + repeat('*', text.length() - 7) + text.substring(text.length() - 4);
    }

    private String maskEmail(String text) {
        int at = text.indexOf('@');
        if (at <= 0) {
            return maskMiddle(text);
        }
        return maskMiddle(text.substring(0, at)) + text.substring(at);
    }

    private String maskMiddle(String text) {
        if (text.length() <= 2) {
            return repeat('*', text.length());
        }
        return text.charAt(0) + repeat('*', text.length() - 2) + text.charAt(text.length() - 1);
    }

    private String repeat(char value, int count) {
        return String.valueOf(value).repeat(Math.max(0, count));
    }
}
