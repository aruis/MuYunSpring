package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** One typed hop after the direct {@link ReferenceLoad#source()} target. */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceHop {
    Class<?> target();

    /** Optional foreign-key field on the current hop source for disambiguation. */
    String via() default "";
}
