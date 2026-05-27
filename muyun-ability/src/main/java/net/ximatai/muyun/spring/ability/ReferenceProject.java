package net.ximatai.muyun.spring.ability;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceProject {
    String targetField();

    String outputField();
}
