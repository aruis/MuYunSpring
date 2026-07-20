package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewService;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckService;
import net.ximatai.muyun.spring.platform.generation.ReferenceRecordGenerationFacade;
import net.ximatai.muyun.spring.platform.ui.PlatformRecordNavigationService;

public record DynamicRecordActionServices(
        CodeBusinessPreviewService codeBusinessPreviewService,
        ReferenceRecordGenerationFacade referenceRecordGenerationFacade,
        RecordDuplicateCheckService duplicateCheckService,
        PlatformRecordNavigationService navigationService
) {
}
