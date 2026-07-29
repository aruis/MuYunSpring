package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares one canonical service operation that participates in the platform runtime. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformOperation {
    PlatformAction value();
}
