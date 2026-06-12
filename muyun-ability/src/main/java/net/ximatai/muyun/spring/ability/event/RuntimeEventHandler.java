package net.ximatai.muyun.spring.ability.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeEventHandler {
    RuntimeEventType event();

    String entityAlias() default "";

    String actionCode() default "";

    int order() default 0;

    RuntimeEventHandlerPhase phase() default RuntimeEventHandlerPhase.DEFAULT;

    RuntimeEventHandlerFailurePolicy failure() default RuntimeEventHandlerFailurePolicy.DEFAULT;
}
