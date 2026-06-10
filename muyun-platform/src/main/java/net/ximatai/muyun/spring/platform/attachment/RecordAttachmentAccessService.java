package net.ximatai.muyun.spring.platform.attachment;

public interface RecordAttachmentAccessService {
    RecordAttachmentAccess issueUploadAccess(String moduleAlias, String recordId);

    RecordAttachmentAccess issuePreviewAccess(String moduleAlias, String recordId, RecordAttachment attachment);

    RecordAttachmentAccess issueDownloadAccess(String moduleAlias, String recordId, RecordAttachment attachment);
}
