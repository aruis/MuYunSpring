package net.ximatai.muyun.spring.platform.initialdata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a model field role for initial data reconciliation.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InitialDataRole {
    InitialDataFieldRole value();
}
