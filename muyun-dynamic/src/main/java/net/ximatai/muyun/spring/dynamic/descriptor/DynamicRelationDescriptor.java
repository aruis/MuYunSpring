package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;

public record DynamicRelationDescriptor(
        String code,
        String parentEntity,
        String childEntity,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public static DynamicRelationDescriptor from(EntityRelationDefinition relation) {
        return new DynamicRelationDescriptor(
                relation.code(),
                relation.parentEntity(),
                relation.childEntity(),
                relation.childForeignKeyField(),
                relation.autoPopulate(),
                relation.autoDeleteWithParent()
        );
    }
}
