package net.ximatai.muyun.spring.common.money;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the money contract of a static model field so the platform can compile it
 * into the shared dynamic field definition model for describe, validation and governance.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MoneyField {
    Mode currencyMode() default Mode.SELECTABLE;

    String fixedCurrencyCode() default "";

    String defaultCurrencyCode() default "";

    String currencyFieldName() default "";

    String baseAmountFieldName();

    String baseCurrencyCode() default "";

    String rateTypeCode();

    String rateDateFieldName() default "";

    String exchangeRateFieldName() default "";

    boolean currencyRequired() default true;

    enum Mode {
        FIXED,
        SELECTABLE
    }
}
