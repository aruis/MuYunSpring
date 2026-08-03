package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record OptionFieldDefinition(String fieldName,
                                    OptionBinding binding,
                                    OptionSelectionMode selectionMode) {
    public OptionFieldDefinition {
        fieldName = Preconditions.requireText(fieldName, "optionFieldName");
        if (binding == null) {
            throw new IllegalArgumentException("option binding must not be null");
        }
        selectionMode = selectionMode == null ? OptionSelectionMode.SINGLE : selectionMode;
    }
}
