package net.ximatai.muyun.spring.ability.child;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the aggregate child collection of a parent model. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Children {
    String relationCode() default "";
}
