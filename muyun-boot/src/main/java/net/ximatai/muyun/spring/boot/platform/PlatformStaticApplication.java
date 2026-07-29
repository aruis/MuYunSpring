package net.ximatai.muyun.spring.boot.platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares one application delivered by a static business module bundle. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformStaticApplication {
    String alias();

    String title();

    int sortOrder() default 100;
}
