package net.ximatai.muyun.spring.ability.event;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionEventPayload {
    public static final String EXECUTOR_TYPE = "executorType";
    public static final String AVAILABLE = "available";
    public static final String RESULT_TYPE = "resultType";
    public static final String INTERACTION_ONLY = "interactionOnly";
    public static final String MESSAGE = "message";
    public static final String REFRESH = "refresh";
    public static final String REDIRECT_TO = "redirectTo";
    public static final String RESULT = "result";
    public static final String FAILURE_STAGE = "failureStage";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_TYPE = "errorType";

    private ActionEventPayload() {
    }

    public static Map<String, Object> executed(String executorType,
                                               String resultType,
                                               String message,
                                               boolean refresh,
                                               String redirectTo,
                                               boolean interactionOnly,
                                               Object simpleResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, EXECUTOR_TYPE, executorType);
        putText(payload, RESULT_TYPE, resultType);
        if (interactionOnly) {
            payload.put(INTERACTION_ONLY, true);
        }
        putText(payload, MESSAGE, message);
        if (refresh) {
            payload.put(REFRESH, true);
        }
        putText(payload, REDIRECT_TO, redirectTo);
        if (simpleResult != null) {
            payload.put(RESULT, simpleResult);
        }
        return Map.copyOf(payload);
    }

    public static Map<String, Object> failed(String executorType,
                                             boolean available,
                                             String failureStage,
                                             String errorMessage,
                                             String errorType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, EXECUTOR_TYPE, executorType);
        payload.put(AVAILABLE, available);
        putText(payload, FAILURE_STAGE, failureStage);
        putText(payload, ERROR_MESSAGE, errorMessage);
        putText(payload, ERROR_TYPE, errorType);
        return Map.copyOf(payload);
    }

    public static String text(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static Boolean bool(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return null;
    }

    private static void putText(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}
