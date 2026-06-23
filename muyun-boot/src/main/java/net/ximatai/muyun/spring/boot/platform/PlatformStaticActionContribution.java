package net.ximatai.muyun.spring.boot.platform;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformStaticActionContribution {
    String targetModule();

    String resource();

    String resourceTitle();
}
