package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.web.*;

import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.platform.deletion.PurgeEntryResult;
import net.ximatai.muyun.spring.platform.deletion.PurgeReport;
import net.ximatai.muyun.spring.platform.deletion.RestoreEntryResult;
import net.ximatai.muyun.spring.platform.deletion.RestoreReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecycleBinMutationResultSupportTest {
    @Test
    void shouldReportLabeledRestoreSuccessAndRecordChange() {
        MutationContext context = new MutationContext();
        RestoreReport report = new RestoreReport("delete-1", "restore-1", List.of(
                new RestoreEntryResult("entry-1", "iam.tenant", "tenant", "tenant-1",
                        RestoreEntryResult.Status.RESTORED, null)
        ));

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            RecycleBinMutationResultSupport.restored("iam.tenant", "tenant-1", "演示租户", report);
        }

        assertThat(context.message().code()).isEqualTo("platform.recycle-bin.restored");
        assertThat(context.message().text()).isEqualTo("「演示租户」恢复成功");
        assertThat(context.committedChangeSet(Class::getSimpleName).changes()).singleElement()
                .satisfies(change -> {
                    assertThat(change.type()).isEqualTo("record-updated");
                    assertThat(change.recordId()).isEqualTo("tenant-1");
                });
    }

    @Test
    void shouldKeepRootRecordWhenOnlyChildWasPurged() {
        MutationContext context = new MutationContext();
        PurgeReport report = new PurgeReport("delete-1", "purge-1", List.of(
                new PurgeEntryResult("entry-2", "iam.application", "application", "app-1",
                        PurgeEntryResult.Status.PURGED, null),
                new PurgeEntryResult("entry-1", "iam.tenant", "tenant", "tenant-1",
                        PurgeEntryResult.Status.SKIPPED, "child purge failed")
        ));

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            RecycleBinMutationResultSupport.purged("iam.tenant", "tenant-1", "演示租户", report);
        }

        assertThat(context.message().type().name()).isEqualTo("WARNING");
        assertThat(context.message().text())
                .isEqualTo("「演示租户」彻底删除未完成：成功 1，跳过 1，失败 0");
        assertThat(context.committedChangeSet(Class::getSimpleName).changes()).isEmpty();
    }

    @Test
    void shouldNotReportDataChangeWhenRestoreDidNotChangeAnyRecord() {
        MutationContext context = new MutationContext();
        RestoreReport report = new RestoreReport("delete-1", "restore-1", List.of(
                new RestoreEntryResult("entry-1", "iam.tenant", "tenant", "tenant-1",
                        RestoreEntryResult.Status.FAILED, "conflict")
        ));

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            RecycleBinMutationResultSupport.restored("iam.tenant", "tenant-1", "演示租户", report);
        }

        assertThat(context.message().text())
                .isEqualTo("「演示租户」恢复未完成：成功 0，跳过 0，失败 1");
        assertThat(context.committedChangeSet(Class::getSimpleName).changes()).isEmpty();
    }
}
