package net.ximatai.muyun.spring.web;

import java.util.function.IntSupplier;

public final class StandardMutationResultSupport {
    private StandardMutationResultSupport() {
    }

    public static void created(ScopedWeb<?> web, String recordId) {
        created(web.webScopeName(), recordId);
    }

    public static void created(ScopedWeb<?> web, String recordId, String recordLabel) {
        StaticCrudActionResultSupport.created(web.webScopeName(), recordId, recordLabel);
    }

    public static void updated(ScopedWeb<?> web, String recordId) {
        updated(web.webScopeName(), recordId);
    }

    public static void updated(ScopedWeb<?> web, String recordId, String recordLabel) {
        StaticCrudActionResultSupport.updated(web.webScopeName(), recordId, recordLabel);
    }

    public static int deleted(ScopedWeb<?> web, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> deleted(web.webScopeName(), id));
    }

    public static int deleted(ScopedWeb<?> web, String recordId, String recordLabel, IntSupplier action) {
        return countMutation(recordId, action, id -> StaticCrudActionResultSupport.deleted(web.webScopeName(), id, recordLabel));
    }

    public static int enabled(ScopedWeb<?> web, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> enabled(web.webScopeName(), id));
    }

    public static int enabled(ScopedWeb<?> web, String recordId, String recordLabel, IntSupplier action) {
        return countMutation(recordId, action, id -> StaticCrudActionResultSupport.enabled(web.webScopeName(), id, recordLabel));
    }

    public static int disabled(ScopedWeb<?> web, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> disabled(web.webScopeName(), id));
    }

    public static int disabled(ScopedWeb<?> web, String recordId, String recordLabel, IntSupplier action) {
        return countMutation(recordId, action, id -> StaticCrudActionResultSupport.disabled(web.webScopeName(), id, recordLabel));
    }

    public static int sorted(ScopedWeb<?> web, IntSupplier action) {
        int count = action.getAsInt();
        if (count > 0) {
            sorted(web.webScopeName());
        }
        return count;
    }

    public static int deleted(String moduleAlias, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> deleted(moduleAlias, id));
    }

    public static int enabled(String moduleAlias, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> enabled(moduleAlias, id));
    }

    public static int disabled(String moduleAlias, String recordId, IntSupplier action) {
        return countMutation(recordId, action, id -> disabled(moduleAlias, id));
    }

    public static int sorted(String moduleAlias, IntSupplier action) {
        int count = action.getAsInt();
        if (count > 0) {
            sorted(moduleAlias);
        }
        return count;
    }

    public static void created(String moduleAlias, String recordId) {
        StaticCrudActionResultSupport.created(moduleAlias, recordId);
    }

    public static void updated(String moduleAlias, String recordId) {
        StaticCrudActionResultSupport.updated(moduleAlias, recordId);
    }

    public static void deleted(String moduleAlias, String recordId) {
        StaticCrudActionResultSupport.deleted(moduleAlias, recordId);
    }

    public static void enabled(String moduleAlias, String recordId) {
        StaticCrudActionResultSupport.enabled(moduleAlias, recordId);
    }

    public static void disabled(String moduleAlias, String recordId) {
        StaticCrudActionResultSupport.disabled(moduleAlias, recordId);
    }

    public static void sorted(String moduleAlias) {
        StaticCrudActionResultSupport.sorted(moduleAlias);
    }

    private static int countMutation(String recordId,
                                     IntSupplier action,
                                     java.util.function.Consumer<String> successReporter) {
        int count = action.getAsInt();
        if (count > 0) {
            successReporter.accept(recordId);
        }
        return count;
    }
}
