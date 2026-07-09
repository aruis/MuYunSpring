package net.ximatai.muyun.spring.ability.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleReference {
    String code() default "";

    Class<?> target() default Void.class;

    String targetModuleAlias() default "";

    String targetField() default "id";
}
