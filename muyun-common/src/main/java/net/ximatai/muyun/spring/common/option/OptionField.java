package net.ximatai.muyun.spring.common.option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionField {
    OptionSourceType type();

    String source();

    OptionSelectionMode selectionMode() default OptionSelectionMode.SINGLE;

    OptionTitleOutput titleOutput() default OptionTitleOutput.AUTO;

    String titleOutputField() default "";
}
