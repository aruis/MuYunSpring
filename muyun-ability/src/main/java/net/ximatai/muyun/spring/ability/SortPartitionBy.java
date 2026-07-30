package net.ximatai.muyun.spring.ability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the persisted business fields that partition an entity's order sequence.
 * The service still declares {@link SortAbility}; this annotation supplies its
 * model-shaped configuration without leaking query construction into the service.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SortPartitionBy {
    String[] fields();

    String message() default "";
}
