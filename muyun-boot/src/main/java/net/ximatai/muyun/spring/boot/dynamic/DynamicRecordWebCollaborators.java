package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.boot.platform.DynamicRelationProjectionReadService;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentAccessService;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentService;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewService;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckService;
import net.ximatai.muyun.spring.platform.generation.ReferenceRecordGenerationFacade;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import net.ximatai.muyun.spring.platform.ui.PlatformRecordNavigationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class DynamicRecordWebCollaborators {
    private final CodeBusinessPreviewService codeBusinessPreviewService;
    private final ReferenceRecordGenerationFacade referenceRecordGenerationFacade;
    private final PlatformPageConfigSnapshotService pageConfigSnapshotService;
    private final PlatformQueryItemService queryItemService;
    private final ModuleMetadataFieldService moduleMetadataFieldService;
    private final RecordAttachmentService recordAttachmentService;
    private final RecordAttachmentAccessService recordAttachmentAccessService;
    private final RecordDuplicateCheckService duplicateCheckService;
    private final PlatformRecordNavigationService navigationService;
    private final DynamicRelationProjectionReadService relationProjectionReadService;

    public DynamicRecordWebCollaborators(
            ObjectProvider<CodeBusinessPreviewService> codeBusinessPreviewService,
            ObjectProvider<ReferenceRecordGenerationFacade> referenceRecordGenerationFacade,
            ObjectProvider<PlatformPageConfigSnapshotService> pageConfigSnapshotService,
            ObjectProvider<PlatformQueryItemService> queryItemService,
            ObjectProvider<ModuleMetadataFieldService> moduleMetadataFieldService,
            ObjectProvider<RecordAttachmentService> recordAttachmentService,
            ObjectProvider<RecordAttachmentAccessService> recordAttachmentAccessService,
            ObjectProvider<RecordDuplicateCheckService> duplicateCheckService,
            ObjectProvider<PlatformRecordNavigationService> navigationService,
            ObjectProvider<DynamicRelationProjectionReadService> relationProjectionReadService) {
        this.codeBusinessPreviewService = available(codeBusinessPreviewService);
        this.referenceRecordGenerationFacade = available(referenceRecordGenerationFacade);
        this.pageConfigSnapshotService = available(pageConfigSnapshotService);
        this.queryItemService = available(queryItemService);
        this.moduleMetadataFieldService = available(moduleMetadataFieldService);
        this.recordAttachmentService = available(recordAttachmentService);
        this.recordAttachmentAccessService = available(recordAttachmentAccessService);
        this.duplicateCheckService = available(duplicateCheckService);
        this.navigationService = available(navigationService);
        DynamicRelationProjectionReadService projectionReadService = available(relationProjectionReadService);
        this.relationProjectionReadService = projectionReadService == null
                ? new DynamicRelationProjectionReadService()
                : projectionReadService;
    }

    public CodeBusinessPreviewService codeBusinessPreviewService() {
        return codeBusinessPreviewService;
    }

    public ReferenceRecordGenerationFacade referenceRecordGenerationFacade() {
        return referenceRecordGenerationFacade;
    }

    public PlatformPageConfigSnapshotService pageConfigSnapshotService() {
        return pageConfigSnapshotService;
    }

    public PlatformQueryItemService queryItemService() {
        return queryItemService;
    }

    public ModuleMetadataFieldService moduleMetadataFieldService() {
        return moduleMetadataFieldService;
    }

    public RecordAttachmentService recordAttachmentService() {
        return recordAttachmentService;
    }

    public RecordAttachmentAccessService recordAttachmentAccessService() {
        return recordAttachmentAccessService;
    }

    public RecordDuplicateCheckService duplicateCheckService() {
        return duplicateCheckService;
    }

    public PlatformRecordNavigationService navigationService() {
        return navigationService;
    }

    public DynamicRelationProjectionReadService relationProjectionReadService() {
        return relationProjectionReadService;
    }

    private static <T> T available(ObjectProvider<T> provider) {
        return provider == null ? null : provider.getIfAvailable();
    }
}
