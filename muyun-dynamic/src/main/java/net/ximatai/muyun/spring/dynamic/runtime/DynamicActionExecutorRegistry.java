package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

public final class DynamicActionExecutorRegistry {
    private final Supplier<Collection<DynamicActionExecutor>> executorSupplier;
    private final Map<String, DynamicActionExecutor> localExecutors = new LinkedHashMap<>();

    public DynamicActionExecutorRegistry() {
        this(Map.of());
    }

    public DynamicActionExecutorRegistry(Collection<DynamicActionExecutor> executors) {
        this(toExecutorMap(executors));
    }

    private DynamicActionExecutorRegistry(Map<String, DynamicActionExecutor> executors) {
        Map<String, DynamicActionExecutor> copied = Map.copyOf(executors);
        this.executorSupplier = copied::values;
    }

    public DynamicActionExecutorRegistry(Supplier<Collection<DynamicActionExecutor>> executorSupplier) {
        this.executorSupplier = executorSupplier == null ? java.util.List::of : executorSupplier;
    }

    public DynamicActionExecutorRegistry register(DynamicActionExecutor executor) {
        register(localExecutors, executor);
        return this;
    }

    public DynamicActionExecutor require(String executorKey) {
        String key = requireKey(executorKey);
        DynamicActionExecutor executor = executorMap().get(key);
        if (executor == null) {
            throw new IllegalArgumentException("unknown dynamic action executor key: " + key);
        }
        return executor;
    }

    public boolean contains(String executorKey) {
        return executorKey != null && executorMap().containsKey(executorKey);
    }

    public DynamicActionExecutorDefinition definition(String executorKey) {
        return require(executorKey).definition();
    }

    public List<DynamicActionExecutorDefinition> definitions() {
        return executorMap().values().stream()
                .map(DynamicActionExecutor::definition)
                .sorted(java.util.Comparator.comparing(DynamicActionExecutorDefinition::title)
                        .thenComparing(DynamicActionExecutorDefinition::executorKey))
                .toList();
    }

    public static DynamicActionExecutorRegistry empty() {
        return new DynamicActionExecutorRegistry();
    }

    private Map<String, DynamicActionExecutor> executorMap() {
        Map<String, DynamicActionExecutor> registered = toExecutorMap(executorSupplier.get());
        localExecutors.values().forEach(executor -> register(registered, executor));
        return registered;
    }

    private static Map<String, DynamicActionExecutor> toExecutorMap(Collection<DynamicActionExecutor> executors) {
        Map<String, DynamicActionExecutor> registered = new LinkedHashMap<>();
        if (executors != null) {
            executors.forEach(executor -> register(registered, executor));
        }
        return registered;
    }

    private static void register(Map<String, DynamicActionExecutor> registered, DynamicActionExecutor executor) {
        if (executor == null) {
            return;
        }
        String key = requireKey(executor.executorKey());
        DynamicActionExecutorDefinition definition = executor.definition();
        if (definition == null || !key.equals(requireKey(definition.executorKey()))) {
            throw new IllegalArgumentException("dynamic action executor definition key must match executor key: " + key);
        }
        DynamicActionExecutor previous = registered.putIfAbsent(key, executor);
        if (previous != null) {
            throw new IllegalArgumentException("duplicate dynamic action executor key: " + key);
        }
    }

    private static String requireKey(String executorKey) {
        if (executorKey == null || executorKey.isBlank()) {
            throw new IllegalArgumentException("dynamic action executorKey must not be blank");
        }
        if (!executorKey.equals(executorKey.trim())) {
            throw new IllegalArgumentException("dynamic action executorKey must not contain leading or trailing spaces");
        }
        if (executorKey.length() > 128) {
            throw new IllegalArgumentException("dynamic action executorKey must not exceed 128 characters");
        }
        return executorKey;
    }
}
