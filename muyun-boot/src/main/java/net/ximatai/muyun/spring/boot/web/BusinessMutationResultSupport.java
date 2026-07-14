package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;

public final class BusinessMutationResultSupport {
    private BusinessMutationResultSupport() {
    }

    public static void success(String code, String text) {
        MutationContextHolder.current()
                .ifPresent(context -> context.message(ActionMessage.success(code, text)));
    }

    public static void updated(String moduleAlias, String recordId) {
        MutationContextHolder.current()
                .ifPresent(context -> context.record(DataChange.recordUpdated(moduleAlias, recordId)));
    }

    public static void collectionChanged(String moduleAlias) {
        MutationContextHolder.current()
                .ifPresent(context -> context.record(DataChange.collectionChanged(moduleAlias)));
    }
}
