package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionField {
    OptionSourceType type();

    String source() default "";

    Class<? extends CodeTitleEnum> enumType() default CodeTitleEnum.class;

    OptionSelectionMode selectionMode() default OptionSelectionMode.SINGLE;
}
