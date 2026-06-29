package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record OptionFieldDefinition(String fieldName,
                                    OptionBinding binding,
                                    OptionSelectionMode selectionMode,
                                    OptionTitleOutput titleOutput,
                                    String titleOutputField) {
    public OptionFieldDefinition {
        fieldName = Preconditions.requireText(fieldName, "optionFieldName");
        if (binding == null) {
            throw new IllegalArgumentException("option binding must not be null");
        }
        selectionMode = selectionMode == null ? OptionSelectionMode.SINGLE : selectionMode;
        titleOutput = titleOutput == null ? OptionTitleOutput.AUTO : titleOutput;
        titleOutputField = titleOutputField == null ? "" : titleOutputField.trim();
        if (titleOutput == OptionTitleOutput.CUSTOM) {
            titleOutputField = Preconditions.requireText(titleOutputField, "optionTitleOutputField");
        } else if (titleOutput == OptionTitleOutput.NONE) {
            titleOutputField = "";
        } else if (titleOutputField.isBlank()) {
            titleOutputField = fieldName + "Title";
        }
    }

    public boolean hasTitleOutput() {
        return titleOutput != OptionTitleOutput.NONE;
    }
}
