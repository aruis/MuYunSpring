package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceTo {
    String moduleAlias();

    String entityAlias();

    ReferenceCardinality cardinality() default ReferenceCardinality.ONE;

    boolean autoTitle() default false;

    String titleOutputField() default "";

    ReferenceProject[] projections() default {};
}
