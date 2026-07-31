package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;

/**
 * Source-neutral option-field facts exposed to a resolved UI descriptor.
 * Actual options stay runtime data because dictionary scope and enabled state are tenant-sensitive.
 */
public record ResolvedOptionFieldDescriptor(OptionBinding binding,
                                            OptionSelectionMode selectionMode,
                                            String titleField) {
    public ResolvedOptionFieldDescriptor {
        if (binding == null) {
            throw new IllegalArgumentException("option binding must not be null");
        }
        selectionMode = selectionMode == null ? OptionSelectionMode.SINGLE : selectionMode;
        titleField = titleField == null || titleField.isBlank() ? null : titleField.trim();
    }
}
