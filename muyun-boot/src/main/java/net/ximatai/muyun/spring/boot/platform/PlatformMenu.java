package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.menu.MenuOpenMode;

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

    MenuOpenMode openMode() default MenuOpenMode.TAB;

    String route() default "";

    boolean enabled() default true;
}
