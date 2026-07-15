package net.ximatai.muyun.spring.ability.action;

import net.ximatai.muyun.spring.ability.TransactionScopeSupport;
import net.ximatai.muyun.spring.common.id.Ids;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MutationContext {
    private final String changeSetId;
    private final List<DataChangeIntent> intents = new ArrayList<>();
    private final List<DataChange> directChanges = new ArrayList<>();
    private ActionMessage message;
    private boolean commitHookRegistered;
    private boolean transactionCommitted;
    private CommittedChangeSet committedChangeSet;
    private DataChangeModuleAliasResolver commitResolver;

    public MutationContext() {
        this(Ids.newId());
    }

    MutationContext(String changeSetId) {
        this.changeSetId = Objects.requireNonNull(changeSetId, "changeSetId must not be null");
    }

    public String changeSetId() {
        return changeSetId;
    }

    public synchronized ActionMessage message() {
        return message;
    }

    public synchronized void message(ActionMessage message) {
        if (message != null) {
            this.message = message;
        }
    }

    public synchronized void record(DataChangeIntent intent) {
        intents.add(Objects.requireNonNull(intent, "intent must not be null"));
        registerCommitHookIfNeeded();
    }

    public synchronized void record(DataChange change) {
        directChanges.add(Objects.requireNonNull(change, "change must not be null"));
        registerCommitHookIfNeeded();
    }

    public synchronized CommittedChangeSet committedChangeSet(DataChangeModuleAliasResolver resolver) {
        if (resolver != null) {
            this.commitResolver = resolver;
        }
        if (committedChangeSet != null) {
            return committedChangeSet;
        }
        if (TransactionScopeSupport.isTransactionActive() && !transactionCommitted) {
            return CommittedChangeSet.empty(changeSetId);
        }
        if (commitHookRegistered && !transactionCommitted) {
            committedChangeSet = CommittedChangeSet.empty(changeSetId);
            return committedChangeSet;
        }
        committedChangeSet = commit(commitResolver);
        return committedChangeSet;
    }

    public void afterCommit(DataChangeModuleAliasResolver resolver, Consumer<CommittedChangeSet> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        if (!TransactionScopeSupport.isTransactionActive()) {
            consumer.accept(committedChangeSet(resolver));
            return;
        }
        TransactionScopeSupport.afterCommitOrNow(() -> {
            synchronized (this) {
                transactionCommitted = true;
            }
            consumer.accept(committedChangeSet(resolver));
        });
    }

    private void registerCommitHookIfNeeded() {
        if (!TransactionScopeSupport.isTransactionActive()) {
            return;
        }
        if (commitHookRegistered) {
            return;
        }
        commitHookRegistered = true;
        TransactionScopeSupport.afterCommitOrNow(() -> {
            synchronized (this) {
                transactionCommitted = true;
            }
        });
    }

    private CommittedChangeSet commit(DataChangeModuleAliasResolver resolver) {
        if (intents.isEmpty() && directChanges.isEmpty()) {
            return CommittedChangeSet.empty(changeSetId);
        }
        LinkedHashMap<String, DataChange> changes = new LinkedHashMap<>();
        for (DataChange change : directChanges) {
            changes.putIfAbsent(changeKey(change), change);
        }
        for (DataChangeIntent intent : intents) {
            DataChange change = toChange(intent, resolver);
            changes.putIfAbsent(changeKey(change), change);
        }
        return new CommittedChangeSet(changeSetId, List.copyOf(changes.values()));
    }

    private DataChange toChange(DataChangeIntent intent, DataChangeModuleAliasResolver resolver) {
        String moduleAlias = resolver == null
                ? moduleAliasFromConstant(intent.moduleType())
                : resolver.moduleAlias(intent.moduleType());
        return switch (intent.operation()) {
            case CREATED -> DataChange.recordCreated(moduleAlias, intent.recordId());
            case UPDATED -> DataChange.recordUpdated(moduleAlias, intent.recordId());
            case DELETED -> DataChange.recordDeleted(moduleAlias, intent.recordId());
            case COLLECTION_CHANGED -> DataChange.collectionChanged(moduleAlias);
        };
    }

    private String moduleAliasFromConstant(Class<?> moduleType) {
        try {
            Object value = moduleType.getField("MODULE_ALIAS").get(null);
            if (value instanceof String alias && !alias.isBlank()) {
                return alias.trim();
            }
        } catch (ReflectiveOperationException ignored) {
            // Resolved later by the Web adapter when available.
        }
        throw new IllegalStateException("cannot resolve module alias from " + moduleType.getName());
    }

    private String changeKey(DataChange change) {
        return String.join("|",
                change.type(),
                change.moduleAlias(),
                value(change.recordId()),
                value(change.resourceKey()),
                value(change.scope()),
                factsKey(change.facts()));
    }

    private String factsKey(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) {
            return "";
        }
        return facts.toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
