package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.util.Preconditions;

/** Declares a virtual field populated from one dictionary-bound source field. */
public record FieldOptionLoadDefinition(String sourceField, String optionItemField) {
    public FieldOptionLoadDefinition {
        sourceField = Preconditions.requireText(sourceField, "sourceField");
        optionItemField = Preconditions.requireText(optionItemField, "optionItemField");
    }

    public FieldOptionLoadDefinition(String sourceField) {
        this(sourceField, "title");
    }
}
