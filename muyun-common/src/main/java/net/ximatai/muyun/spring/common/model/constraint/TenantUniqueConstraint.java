package net.ximatai.muyun.spring.common.model.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a tenant-scoped business identity that the platform must enforce. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TenantUniqueConstraints.class)
public @interface TenantUniqueConstraint {
    String[] fields();

    String message() default "";
}
