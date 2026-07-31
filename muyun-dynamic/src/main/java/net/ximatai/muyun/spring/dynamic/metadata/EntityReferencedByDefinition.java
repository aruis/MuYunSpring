package net.ximatai.muyun.spring.dynamic.metadata;

/** Declares a virtual collection of current-module records that reference this entity. */
public record EntityReferencedByDefinition(
        String targetEntityAlias,
        String sourceEntityAlias,
        String sourceField,
        String outputField
) {
}
