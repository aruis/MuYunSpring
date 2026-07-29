package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Prevents selected standard operations from being published for one concrete service. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DisablePlatformOperations {
    PlatformAction[] value();
}
