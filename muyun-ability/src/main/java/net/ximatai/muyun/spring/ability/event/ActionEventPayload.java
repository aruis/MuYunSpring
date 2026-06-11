package net.ximatai.muyun.spring.ability.event;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionEventPayload {
    public static final String EXECUTOR_TYPE = "executorType";
    public static final String ACTION_LEVEL = "actionLevel";
    public static final String AVAILABLE = "available";
    public static final String RESULT_TYPE = "resultType";
    public static final String INTERACTION_ONLY = "interactionOnly";
    public static final String MESSAGE = "message";
    public static final String REFRESH = "refresh";
    public static final String REFRESH_STRATEGY = "refreshStrategy";
    public static final String REDIRECT_TO = "redirectTo";
    public static final String RESULT = "result";
    public static final String FAILURE_STAGE = "failureStage";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_TYPE = "errorType";
    public static final String SYSTEM_REASON = "systemReason";

    private ActionEventPayload() {
    }

    public static Map<String, Object> executed(String executorType,
                                               String resultType,
                                               String message,
                                               boolean refresh,
                                               String redirectTo,
                                               boolean interactionOnly,
                                               Object simpleResult) {
        return executed(executorType, null, resultType, message, refresh, redirectTo, null,
                interactionOnly, simpleResult);
    }

    public static Map<String, Object> executed(String executorType,
                                               String actionLevel,
                                               String resultType,
                                               String message,
                                               boolean refresh,
                                               String redirectTo,
                                               boolean interactionOnly,
                                               Object simpleResult) {
        return executed(executorType, actionLevel, resultType, message, refresh, redirectTo, null,
                interactionOnly, simpleResult);
    }

    public static Map<String, Object> executed(String executorType,
                                               String actionLevel,
                                               String resultType,
                                               String message,
                                               boolean refresh,
                                               String redirectTo,
                                               Object refreshStrategy,
                                               boolean interactionOnly,
                                               Object simpleResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, EXECUTOR_TYPE, executorType);
        putText(payload, ACTION_LEVEL, actionLevel);
        putText(payload, RESULT_TYPE, resultType);
        if (interactionOnly) {
            payload.put(INTERACTION_ONLY, true);
        }
        putText(payload, MESSAGE, message);
        if (refresh) {
            payload.put(REFRESH, true);
        }
        if (refreshStrategy != null) {
            payload.put(REFRESH_STRATEGY, refreshStrategy);
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
        return failed(executorType, null, available, failureStage, errorMessage, errorType);
    }

    public static Map<String, Object> failed(String executorType,
                                             String actionLevel,
                                             boolean available,
                                             String failureStage,
                                             String errorMessage,
                                             String errorType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, EXECUTOR_TYPE, executorType);
        putText(payload, ACTION_LEVEL, actionLevel);
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

    public static Map<String, Object> withSystemReason(Map<String, Object> payload, String systemReason) {
        if (systemReason == null || systemReason.isBlank()) {
            return payload == null ? Map.of() : payload;
        }
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (payload != null) {
            enriched.putAll(payload);
        }
        enriched.put(SYSTEM_REASON, systemReason);
        return Map.copyOf(enriched);
    }

    private static void putText(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}
