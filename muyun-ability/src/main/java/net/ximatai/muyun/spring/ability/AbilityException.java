package net.ximatai.muyun.spring.ability;

public class AbilityException extends RuntimeException {
    public AbilityException(String message) {
        super(message);
    }

    public AbilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
