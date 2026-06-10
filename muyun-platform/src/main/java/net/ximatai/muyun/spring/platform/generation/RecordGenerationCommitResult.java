package net.ximatai.muyun.spring.platform.generation;

import java.util.List;

public record RecordGenerationCommitResult(
        String ruleId,
        String batchId,
        String targetModuleAlias,
        List<String> recordIds
) {
    public RecordGenerationCommitResult {
        recordIds = recordIds == null ? List.of() : List.copyOf(recordIds);
    }
}
