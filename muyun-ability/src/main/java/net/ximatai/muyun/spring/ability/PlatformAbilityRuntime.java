package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;

public final class PlatformAbilityRuntime {
    private PlatformAbilityRuntime() {
    }

    public static void configureStaticOptionFieldValueValidator(StaticOptionFieldValueValidator validator) {
        PlatformAbilityDispatcher.setStaticOptionFieldValueValidator(validator);
    }

    public static void resetStaticOptionFieldValueValidator() {
        PlatformAbilityDispatcher.resetStaticOptionFieldValueValidator();
    }

    public static void configureDeletionLifecycleListener(DeletionLifecycleListener listener) {
        PlatformAbilityDispatcher.setDeletionLifecycleListener(listener);
    }

    public static void resetDeletionLifecycleListener() {
        PlatformAbilityDispatcher.resetDeletionLifecycleListener();
    }
}
