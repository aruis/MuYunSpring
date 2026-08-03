package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

/** Compiled static option projection declaration. */
public record OptionLoadDefinition(String sourceField,
                                   String outputField,
                                   String optionItemField,
                                   OptionFieldDefinition sourceDefinition) {
    public OptionLoadDefinition {
        sourceField = Preconditions.requireText(sourceField, "optionLoadSourceField");
        outputField = Preconditions.requireText(outputField, "optionLoadOutputField");
        optionItemField = Preconditions.requireText(optionItemField, "optionLoadItemField");
        if (sourceDefinition == null) {
            throw new IllegalArgumentException("option load source definition must not be null");
        }
    }
}
