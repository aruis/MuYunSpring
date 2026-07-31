package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.platform.deletion.PurgeEntryResult;
import net.ximatai.muyun.spring.platform.deletion.PurgeReport;
import net.ximatai.muyun.spring.platform.deletion.RestoreEntryResult;
import net.ximatai.muyun.spring.platform.deletion.RestoreReport;

final class RecycleBinMutationResultSupport {
    private RecycleBinMutationResultSupport() {
    }

    static void restored(String moduleAlias, String recordId, String recordLabel, RestoreReport report) {
        long succeeded = countRestore(report, RestoreEntryResult.Status.RESTORED);
        long skipped = countRestore(report, RestoreEntryResult.Status.SKIPPED);
        long failed = countRestore(report, RestoreEntryResult.Status.FAILED);
        boolean rootSucceeded = restoreStatus(report, recordId) == RestoreEntryResult.Status.RESTORED;
        report(message("platform.recycle-bin.restored", recordLabel, "恢复", rootSucceeded, succeeded, skipped, failed),
                rootSucceeded ? DataChange.recordUpdated(moduleAlias, recordId) : null);
    }

    static void purged(String moduleAlias, String recordId, String recordLabel, PurgeReport report) {
        long succeeded = countPurge(report, PurgeEntryResult.Status.PURGED);
        long skipped = countPurge(report, PurgeEntryResult.Status.SKIPPED);
        long failed = countPurge(report, PurgeEntryResult.Status.FAILED);
        boolean rootSucceeded = purgeStatus(report, recordId) == PurgeEntryResult.Status.PURGED;
        report(message("platform.recycle-bin.purged", recordLabel, "彻底删除", rootSucceeded,
                        succeeded, skipped, failed),
                rootSucceeded ? DataChange.recordDeleted(moduleAlias, recordId) : null);
    }

    private static ActionMessage message(String code,
                                         String recordLabel,
                                         String action,
                                         boolean rootSucceeded,
                                         long succeeded,
                                         long skipped,
                                         long failed) {
        String subject = recordLabel == null || recordLabel.isBlank() ? "" : "「" + recordLabel.trim() + "」";
        if (rootSucceeded && skipped == 0 && failed == 0) {
            return ActionMessage.success(code, subject + action + "成功");
        }
        String details = "成功 " + succeeded + "，跳过 " + skipped + "，失败 " + failed;
        if (rootSucceeded) {
            return ActionMessage.warning(code, subject + action + "完成：" + details);
        }
        return ActionMessage.warning(code, subject + action + "未完成：" + details);
    }

    private static RestoreEntryResult.Status restoreStatus(RestoreReport report, String recordId) {
        if (report == null) return null;
        return report.entries().stream()
                .filter(entry -> java.util.Objects.equals(recordId, entry.recordId()))
                .map(RestoreEntryResult::status)
                .findFirst()
                .orElse(null);
    }

    private static PurgeEntryResult.Status purgeStatus(PurgeReport report, String recordId) {
        if (report == null) return null;
        return report.entries().stream()
                .filter(entry -> java.util.Objects.equals(recordId, entry.recordId()))
                .map(PurgeEntryResult::status)
                .findFirst()
                .orElse(null);
    }

    private static long countRestore(RestoreReport report, RestoreEntryResult.Status status) {
        return report == null ? 0 : report.entries().stream().filter(entry -> entry.status() == status).count();
    }

    private static long countPurge(PurgeReport report, PurgeEntryResult.Status status) {
        return report == null ? 0 : report.entries().stream().filter(entry -> entry.status() == status).count();
    }

    private static void report(ActionMessage message, DataChange change) {
        MutationContextHolder.current().ifPresent(context -> {
            context.message(message);
            if (change != null) {
                context.record(change);
            }
        });
    }
}
