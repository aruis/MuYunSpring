package net.ximatai.muyun.spring.common.option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a field backed by a platform dictionary.
 *
 * <p>When {@link #title()} or {@link #initialItems()} is supplied, the declaration also contributes
 * the dictionary's startup baseline. Runtime option lookup and validation still
 * use the same {@link OptionBinding} contract as {@link OptionField}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictionaryField {
    String source();

    String title() default "";

    int sortOrder() default 100;

    InitialItem[] initialItems() default {};

    OptionSelectionMode selectionMode() default OptionSelectionMode.SINGLE;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface InitialItem {
        String code();

        String title();

        int sortOrder() default 100;
    }
}
