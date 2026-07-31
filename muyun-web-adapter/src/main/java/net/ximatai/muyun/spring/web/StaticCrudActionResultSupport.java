package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;

final class StaticCrudActionResultSupport {
    private StaticCrudActionResultSupport() {
    }

    static void created(String moduleAlias, String recordId) {
        created(moduleAlias, recordId, null);
    }

    static void created(String moduleAlias, String recordId, String recordLabel) {
        report("platform.crud.created", successMessage(recordLabel, "新增"), DataChange.recordCreated(moduleAlias, recordId));
    }

    static void updated(String moduleAlias, String recordId) {
        updated(moduleAlias, recordId, null);
    }

    static void updated(String moduleAlias, String recordId, String recordLabel) {
        report("platform.crud.updated", successMessage(recordLabel, "修改"), DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void deleted(String moduleAlias, String recordId) {
        deleted(moduleAlias, recordId, null);
    }

    static void deleted(String moduleAlias, String recordId, String recordLabel) {
        report("platform.crud.deleted", successMessage(recordLabel, "删除"), DataChange.recordDeleted(moduleAlias, recordId));
    }

    static void enabled(String moduleAlias, String recordId) {
        enabled(moduleAlias, recordId, null);
    }

    static void enabled(String moduleAlias, String recordId, String recordLabel) {
        report("platform.crud.enabled", successMessage(recordLabel, "启用"), DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void disabled(String moduleAlias, String recordId) {
        disabled(moduleAlias, recordId, null);
    }

    static void disabled(String moduleAlias, String recordId, String recordLabel) {
        report("platform.crud.disabled", successMessage(recordLabel, "停用"), DataChange.recordUpdated(moduleAlias, recordId));
    }

    static void sorted(String moduleAlias) {
        report("platform.crud.sorted", "排序成功", DataChange.collectionChanged(moduleAlias));
    }

    private static void report(String code,
                               String text,
                               DataChange change) {
        MutationContextHolder.current().ifPresent(context -> {
            context.message(ActionMessage.success(code, text));
            context.record(change);
        });
    }

    private static String successMessage(String recordLabel, String action) {
        return recordLabel == null || recordLabel.isBlank() ? action + "成功" : "「" + recordLabel.trim() + "」" + action + "成功";
    }
}
