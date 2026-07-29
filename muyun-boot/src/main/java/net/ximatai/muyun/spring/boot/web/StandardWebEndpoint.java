package net.ximatai.muyun.spring.boot.web;

import java.lang.reflect.Method;
import java.util.Set;

/** Identifies MVC methods supplied by the platform's reusable Web adapters. */
public final class StandardWebEndpoint {
    private static final Set<Class<?>> DECLARING_TYPES = Set.of(
            CrudWeb.class,
            ReadOnlyWeb.class,
            ReferenceWeb.class,
            EnableWeb.class,
            SortWeb.class,
            TreeWeb.class,
            RecycleBinWeb.class,
            RecycleBinPurgeWeb.class,
            NestedCrudWebSupport.class
    );

    private StandardWebEndpoint() {
    }

    public static boolean isDefault(Method method) {
        return method != null && DECLARING_TYPES.contains(method.getDeclaringClass());
    }
}
