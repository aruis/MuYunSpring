package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lifecycle rules for a normal reference. It does not model aggregate ownership;
 * use {@code ChildRef} for explicit cascading deletion.
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceIntegrity {
    ReferenceTargetDeletionPolicy onTargetSoftDelete() default ReferenceTargetDeletionPolicy.PRESERVE_HISTORY;
}
