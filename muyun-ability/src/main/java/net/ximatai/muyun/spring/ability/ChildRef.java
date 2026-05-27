package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChildRef {
    String relationCode() default "";

    String parentEntity();

    Class<? extends EntityContract> childModel();

    String childEntity() default "";

    String childForeignKeyField();

    boolean autoPopulate() default true;

    boolean autoDeleteWithParent() default false;
}
