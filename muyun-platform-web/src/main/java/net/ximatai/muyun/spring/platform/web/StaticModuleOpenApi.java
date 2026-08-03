package net.ximatai.muyun.spring.platform.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a static Web module into platform-managed OpenAPI delivery.
 *
 * <p>The annotation is intentionally a delivery declaration rather than a service or CRUD capability.
 * At startup the platform registers the module's exact {@code /openapi} mapping from this declaration.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaticModuleOpenApi {
}
