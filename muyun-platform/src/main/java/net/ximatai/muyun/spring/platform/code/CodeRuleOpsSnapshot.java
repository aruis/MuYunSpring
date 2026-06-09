package net.ximatai.muyun.spring.platform.code;

import java.util.List;

public record CodeRuleOpsSnapshot(
        CodeRule rule,
        List<CodeSequenceState> sequenceStates,
        List<CodeIssueLog> issueLogs,
        List<CodeRecycleEntry> recycleEntries,
        List<CodeLedgerEntry> ledgerEntries
) {
}
