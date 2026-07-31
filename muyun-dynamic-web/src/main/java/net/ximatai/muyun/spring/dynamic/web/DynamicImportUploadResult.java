package net.ximatai.muyun.spring.dynamic.web;

public record DynamicImportUploadResult(
        int created,
        int updated,
        int skipped,
        int errorCount,
        boolean partialSuccess,
        String message,
        String errorFileName,
        String errorFileToken
) {
}
