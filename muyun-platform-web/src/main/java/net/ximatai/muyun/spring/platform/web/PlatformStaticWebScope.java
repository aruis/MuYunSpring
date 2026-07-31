package net.ximatai.muyun.spring.platform.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares how a static module's HTTP projection is scoped.
 *
 * <p>Absent on a controller, its class-level {@code @RequestMapping} must be
 * {@code /<moduleAlias>}. Declare {@link Scope#CUSTOM} only when a deliberately
 * non-canonical HTTP path is part of the Web delivery contract.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformStaticWebScope {
    Scope value() default Scope.MODULE_ALIAS;

    enum Scope {
        MODULE_ALIAS,
        CUSTOM
    }
}
