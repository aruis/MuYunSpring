package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;

final class StaticCrudActionResultSupport {
    private StaticCrudActionResultSupport() {
    }

    static void created(String moduleAlias, String recordId) {
        report("platform.crud.created", "新增成功", DataChange.recordCreated(moduleAlias, recordId));
    }

    static void updated(String moduleAlias, String recordId) {
        report("platform.crud.updated", "修改成功", DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void deleted(String moduleAlias, String recordId) {
        report("platform.crud.deleted", "删除成功", DataChange.recordDeleted(moduleAlias, recordId));
    }

    static void enabled(String moduleAlias, String recordId) {
        report("platform.crud.enabled", "启用成功", DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void disabled(String moduleAlias, String recordId) {
        report("platform.crud.disabled", "停用成功", DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void sorted(String moduleAlias, String recordId) {
        report("platform.crud.sorted", "排序成功", DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void report(StandardMutationKind kind,
                       String moduleAlias,
                       String recordId) {
        switch (kind) {
            case CREATE -> created(moduleAlias, recordId);
            case UPDATE -> updated(moduleAlias, recordId);
            case DELETE -> deleted(moduleAlias, recordId);
            case ENABLE -> enabled(moduleAlias, recordId);
            case DISABLE -> disabled(moduleAlias, recordId);
            case SORT -> sorted(moduleAlias, recordId);
        }
    }

    private static void report(String code,
                               String text,
                               DataChange change) {
        MutationContextHolder.current().ifPresent(context -> {
            context.message(ActionMessage.success(code, text));
            context.record(change);
        });
    }
}
