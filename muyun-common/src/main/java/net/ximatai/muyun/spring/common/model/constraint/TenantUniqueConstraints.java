package net.ximatai.muyun.spring.common.model.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantUniqueConstraints {
    TenantUniqueConstraint[] value();
}
