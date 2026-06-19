package net.ximatai.muyun.spring.boot.platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformMenu {
    String id() default "";

    String parent();

    String title() default "";

    int order() default 100;

    boolean enabled() default true;
}
