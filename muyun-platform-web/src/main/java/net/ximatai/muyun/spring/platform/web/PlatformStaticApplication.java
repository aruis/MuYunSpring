package net.ximatai.muyun.spring.platform.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;

/**
 * Declares and self-registers one application delivered by a static business module bundle.
 *
 * <p>The annotated class is the stable Java identity of the application. It must not be a
 * general-purpose Boot configuration class.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration(proxyBeanMethods = false)
public @interface PlatformStaticApplication {
    String alias();

    String title();

    int sortOrder() default 100;
}
