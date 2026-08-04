package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a code-owned action contributed by a {@code DynamicActionExecutor} to a dynamic module. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PlatformDynamicActionContributions.class)
public @interface PlatformDynamicActionContribution {
    String moduleAlias();

    String actionCode();

    String title();

    String entityAlias() default "";

    String permissionActionCode() default "";

    EntityActionCategory category() default EntityActionCategory.CUSTOM;

    EntityActionLevel actionLevel() default EntityActionLevel.ANY;

    EntityActionAccessMode accessMode() default EntityActionAccessMode.AUTH_REQUIRED;

    boolean actionAuth() default true;

    boolean dataAuth() default false;

    ActionDefaultGrantPolicy defaultGrantPolicy() default ActionDefaultGrantPolicy.NONE;

    String availableExpression() default "";

    String unavailableMessage() default "";
}
