package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.common.exception.ErrorTarget;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts database integrity errors, which are commonly wrapped by JDBI or a
 * transaction interceptor, to safe API-facing descriptions.  It deliberately
 * depends only on JDBC SQL states so individual database drivers remain an
 * implementation detail of applications using the platform.
 */
final class DatabaseConstraintViolationResolver {
    private static final String NOT_NULL_VIOLATION = "23502";

    private DatabaseConstraintViolationResolver() {
    }

    static Optional<ResolvedViolation> resolve(Throwable failure) {
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        Throwable current = failure;
        while (current != null && visited.put(current, Boolean.TRUE) == null) {
            if (current instanceof SQLException sqlException
                    && NOT_NULL_VIOLATION.equals(sqlException.getSQLState())) {
                String column = extractColumn(sqlException);
                List<ErrorTarget> targets = column == null ? List.of() : List.of(ErrorTarget.field(column));
                return Optional.of(new ResolvedViolation("字段为必填，请补充后重试", targets));
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    /**
     * PostgreSQL exposes the column in ServerErrorMessage. Use reflection so
     * this web adapter does not acquire a compile-time PostgreSQL dependency.
     */
    private static String extractColumn(SQLException exception) {
        try {
            Method serverError = exception.getClass().getMethod("getServerErrorMessage");
            Object message = serverError.invoke(exception);
            if (message == null) {
                return null;
            }
            Object column = message.getClass().getMethod("getColumn").invoke(message);
            return column instanceof String value && !value.isBlank() ? value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    record ResolvedViolation(String message, List<ErrorTarget> targets) {
    }
}
