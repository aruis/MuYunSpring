package net.ximatai.muyun.spring.common.web;

import net.ximatai.muyun.spring.common.id.Ids;

import java.util.Optional;

public final class RequestTraceContext {
    public static final String TRACE_ID_HEADER = "X-MuYun-Trace-Id";

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(TRACE_ID.get()).filter(value -> !value.isBlank());
    }

    public static String ensureTraceId() {
        String current = TRACE_ID.get();
        if (current != null && !current.isBlank()) {
            return current;
        }
        String traceId = Ids.newId();
        TRACE_ID.set(traceId);
        return traceId;
    }

    public static Scope use(String traceId) {
        String previous = TRACE_ID.get();
        TRACE_ID.set(normalize(traceId));
        return () -> restore(previous);
    }

    public static void clear() {
        TRACE_ID.remove();
    }

    private static void restore(String previous) {
        if (previous == null || previous.isBlank()) {
            TRACE_ID.remove();
            return;
        }
        TRACE_ID.set(previous);
    }

    private static String normalize(String traceId) {
        return traceId == null || traceId.isBlank() ? Ids.newId() : traceId;
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
