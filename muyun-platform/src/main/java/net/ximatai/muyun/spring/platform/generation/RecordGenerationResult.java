package net.ximatai.muyun.spring.platform.generation;

import java.util.List;

public record RecordGenerationResult(
        String ruleId,
        String actionCode,
        String sourceModuleAlias,
        String sourceRecordId,
        String targetModuleAlias,
        String batchId,
        List<RecordGenerationDraft> drafts
) {
    public RecordGenerationResult {
        drafts = drafts == null ? List.of() : List.copyOf(drafts);
    }
}
