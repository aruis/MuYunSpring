package net.ximatai.muyun.spring.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BusinessMutation
public @interface BusinessMutationResult {
    String code();

    String message();

    BusinessMutationChange change();

    Class<?> module();

    BusinessMutationRecordIdSource recordIdSource() default BusinessMutationRecordIdSource.NONE;

    String recordId() default "";
}
