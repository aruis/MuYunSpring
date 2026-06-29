package net.ximatai.muyun.spring.ability.output;

import net.ximatai.muyun.spring.common.security.FieldOutputContext;

import java.util.Locale;

public record RecordOutputContext(
        FieldOutputContext fieldContext,
        RecordOutputSurface surface,
        String uiConfigId,
        Locale locale
) {
    public RecordOutputContext {
        fieldContext = fieldContext == null ? FieldOutputContext.VIEW : fieldContext;
        surface = surface == null ? surfaceFrom(fieldContext) : surface;
    }

    public static RecordOutputContext of(FieldOutputContext fieldContext) {
        return new RecordOutputContext(fieldContext, surfaceFrom(fieldContext), null, null);
    }

    public static RecordOutputContext list() {
        return of(FieldOutputContext.LIST);
    }

    public static RecordOutputContext view() {
        return of(FieldOutputContext.VIEW);
    }

    public static RecordOutputContext reference() {
        return of(FieldOutputContext.REFERENCE);
    }

    public static RecordOutputContext audit() {
        return of(FieldOutputContext.AUDIT);
    }

    public static RecordOutputContext export() {
        return of(FieldOutputContext.EXPORT);
    }

    public static RecordOutputContext form(String uiConfigId) {
        return new RecordOutputContext(FieldOutputContext.VIEW, RecordOutputSurface.FORM, uiConfigId, null);
    }

    private static RecordOutputSurface surfaceFrom(FieldOutputContext fieldContext) {
        FieldOutputContext normalized = fieldContext == null ? FieldOutputContext.VIEW : fieldContext;
        return switch (normalized) {
            case LIST -> RecordOutputSurface.LIST;
            case REFERENCE -> RecordOutputSurface.REFERENCE;
            case AUDIT -> RecordOutputSurface.AUDIT;
            case EXPORT -> RecordOutputSurface.EXPORT;
            case VIEW -> RecordOutputSurface.VIEW;
        };
    }
}
