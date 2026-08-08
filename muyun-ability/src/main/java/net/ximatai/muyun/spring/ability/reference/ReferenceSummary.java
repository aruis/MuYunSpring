package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Projects selected attributes of a reference as structured summary maps.
 * The generated item always includes {@code id}; {@link #fields()} supplies
 * the remaining attributes.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceSummary {
    String source();

    String[] fields() default {};
}
