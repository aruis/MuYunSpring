package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;

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

    public static void configureReferenceDeletionGuard(ReferenceDeletionGuard guard) {
        PlatformAbilityDispatcher.setReferenceDeletionGuard(guard);
    }

    public static void resetReferenceDeletionGuard() {
        PlatformAbilityDispatcher.resetReferenceDeletionGuard();
    }

    public static void configureReferenceTargetResolver(ReferenceTargetResolver resolver) {
        PlatformAbilityDispatcher.setReferenceTargetResolver(resolver);
    }

    public static void resetReferenceTargetResolver() {
        PlatformAbilityDispatcher.resetReferenceTargetResolver();
    }

    public static ReferenceTargetResolver referenceTargetResolver() {
        return PlatformAbilityDispatcher.referenceTargetResolver();
    }
}
