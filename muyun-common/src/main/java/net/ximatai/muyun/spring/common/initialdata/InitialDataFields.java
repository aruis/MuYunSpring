package net.ximatai.muyun.spring.common.initialdata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares initial data field roles by property name on a model class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InitialDataFields {
    boolean includeId() default true;

    String[] identity() default {};

    String[] managed() default {};

    String[] operator() default {};
}
