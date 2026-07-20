package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentAccessService;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentService;

public record DynamicRecordAttachmentServices(
        RecordAttachmentService attachmentService,
        RecordAttachmentAccessService attachmentAccessService
) {
}
