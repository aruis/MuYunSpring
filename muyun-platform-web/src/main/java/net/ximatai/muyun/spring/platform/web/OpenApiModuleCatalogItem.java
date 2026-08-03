package net.ximatai.muyun.spring.platform.web;

/** A module API document visible to the current caller. */
public record OpenApiModuleCatalogItem(
        String moduleAlias,
        String title,
        String moduleKind,
        String documentPath
) {
}
