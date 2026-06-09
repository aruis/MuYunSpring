package net.ximatai.muyun.spring.platform.exchange.model;

public record ExcelWorkbookMeta(
        String protocolVersion,
        String moduleAlias,
        String uiConfigId,
        String uiConfigTitle,
        String timeZone
) {
}
