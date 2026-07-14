package net.ximatai.muyun.spring.ability.action;

public class DataChangeRecorder {
    public void created(Class<?> moduleType, String recordId) {
        record(DataChangeOperation.CREATED, moduleType, recordId);
    }

    public void updated(Class<?> moduleType, String recordId) {
        record(DataChangeOperation.UPDATED, moduleType, recordId);
    }

    public void deleted(Class<?> moduleType, String recordId) {
        record(DataChangeOperation.DELETED, moduleType, recordId);
    }

    public void collectionChanged(Class<?> moduleType) {
        MutationContextHolder.current()
                .ifPresent(context -> context.record(new DataChangeIntent(
                        DataChangeOperation.COLLECTION_CHANGED, moduleType, "__collection__")));
    }

    private void record(DataChangeOperation operation, Class<?> moduleType, String recordId) {
        MutationContextHolder.current()
                .ifPresent(context -> context.record(new DataChangeIntent(operation, moduleType, recordId)));
    }
}
