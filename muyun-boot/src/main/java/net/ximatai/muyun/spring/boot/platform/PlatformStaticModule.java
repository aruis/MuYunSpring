package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformStaticModule {
    /** Whether the Controller's base HTTP path follows the module alias convention. */
    enum WebScope {
        MODULE_ALIAS,
        CUSTOM
    }

    /** Static application declaration that owns this module. */
    Class<?> application();

    String alias();

    String title();

    String parent() default "";

    String route() default "";

    String externalUrl() default "";

    /**
     * {@link WebScope#MODULE_ALIAS} requires any declared class-level {@code @RequestMapping}
     * paths to consist only of {@code /<moduleAlias>}. Use {@link WebScope#CUSTOM} for
     * resource-scoped, legacy-compatible, or otherwise deliberately non-canonical HTTP paths.
     */
    WebScope webScope() default WebScope.MODULE_ALIAS;

    EntityCapability[] capabilities() default {};
}
