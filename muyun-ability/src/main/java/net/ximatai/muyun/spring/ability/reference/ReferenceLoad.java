package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a non-persistent field populated from a referenced record.
 * The {@code source} must name a field declared with {@link ReferenceTo}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceLoad {
    String source();

    /** Field read from the direct or typed terminal reference target. */
    String field() default "title";

    /** Typed reference hops after the direct source target. */
    ReferenceHop[] hops() default {};
}
