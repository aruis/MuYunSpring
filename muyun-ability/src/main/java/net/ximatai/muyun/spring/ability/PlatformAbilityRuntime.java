package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;

public final class PlatformAbilityRuntime {
    private PlatformAbilityRuntime() {
    }

    public static void configureStaticOptionFieldValueValidator(StaticOptionFieldValueValidator validator) {
        PlatformAbilityDispatcher.setStaticOptionFieldValueValidator(validator);
    }

    public static void resetStaticOptionFieldValueValidator() {
        PlatformAbilityDispatcher.resetStaticOptionFieldValueValidator();
    }
}
