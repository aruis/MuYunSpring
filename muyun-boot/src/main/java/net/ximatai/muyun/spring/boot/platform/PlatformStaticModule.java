package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformStaticModule {
    String application();

    String alias();

    String title();

    String parent() default "";

    EntityCapability[] capabilities() default {};
}
