package net.ximatai.muyun.spring.common.platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomActionEndpoint {
    String value();

    PlatformActionLevel level() default PlatformActionLevel.DEFAULT;

    boolean dataAuth() default false;

    String recordIdPathVariable() default "id";
}
