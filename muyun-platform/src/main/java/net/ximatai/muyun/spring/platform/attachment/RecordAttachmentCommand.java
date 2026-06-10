package net.ximatai.muyun.spring.platform.attachment;

public record RecordAttachmentCommand(
        String id,
        String fileId,
        String displayName,
        Integer sort,
        String remark
) {
}
