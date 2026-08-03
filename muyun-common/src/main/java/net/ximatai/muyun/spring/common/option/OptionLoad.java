package net.ximatai.muyun.spring.common.option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a non-persistent projection from an option field's resolved {@link OptionItem}.
 * The {@code source} must name a field declared with {@link OptionField} or {@link DictionaryField}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionLoad {
    String source();

    /** Stable {@link OptionItem} property to read. */
    String field() default "title";
}
