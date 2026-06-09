package net.ximatai.muyun.spring.platform.code;

public record CodeLedgerInspection(
        CodeLedgerEntry entry,
        String currentValue,
        boolean consistent,
        CodeLedgerInconsistencyReason reason,
        boolean releaseAllowed,
        String summary
) {
}
