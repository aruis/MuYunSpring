package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.util.Preconditions;

public final class DynamicWebRequest {
    private static final ThreadLocal<String> REQUEST_PATH = new ThreadLocal<>();

    private DynamicWebRequest() {
    }

    public static void useRequestPath(String requestPath) {
        REQUEST_PATH.set(requestPath);
    }

    public static void clearRequestPath() {
        REQUEST_PATH.remove();
    }

    static String moduleAlias() {
        return Preconditions.requireText(firstPathSegment(REQUEST_PATH.get()), "moduleAlias");
    }

    private static String firstPathSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int slash = normalized.indexOf('/');
        return slash < 0 ? normalized : normalized.substring(0, slash);
    }
}
