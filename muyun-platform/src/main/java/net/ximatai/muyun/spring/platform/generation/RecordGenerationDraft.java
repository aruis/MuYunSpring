package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.platform.impact.RecordOriginContext;

public record RecordGenerationDraft(
        String targetModuleAlias,
        String targetEntityAlias,
        DynamicRecord record,
        RecordOriginContext originContext
) {
}
