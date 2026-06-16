package net.ximatai.muyun.spring.common.measure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the measure unit contract of a static model field so the platform can compile it
 * into the shared {@code FieldDefinition.measureUnit} model for describe, validation and governance.
 *
 * <p>The annotation is not a static CRUD persistence interceptor. Static services that need automatic
 * base-value normalization must call a platform normalizer explicitly until that write-chain capability
 * is introduced.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MeasureUnitField {
    String categoryAlias();

    Mode mode() default Mode.SELECTABLE;

    String fixedUnitCode() default "";

    String defaultUnitCode() default "";

    String unitFieldName() default "";

    String baseValueFieldName();

    String baseUnitCategoryAlias() default "";

    String baseUnitCode();

    ConversionMode conversionMode() default ConversionMode.LINEAR;

    String conversionScopeFieldName() default "";

    boolean unitRequired() default true;

    enum Mode {
        FIXED,
        SELECTABLE
    }

    enum ConversionMode {
        LINEAR,
        BUSINESS_RULE
    }
}
