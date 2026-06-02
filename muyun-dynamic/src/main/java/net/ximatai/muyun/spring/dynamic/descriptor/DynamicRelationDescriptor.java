package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;

public record DynamicRelationDescriptor(
        String code,
        String parentEntityAlias,
        String childEntityAlias,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public static DynamicRelationDescriptor from(EntityRelationDefinition relation) {
        return new DynamicRelationDescriptor(
                relation.code(),
                relation.parentEntityAlias(),
                relation.childEntityAlias(),
                relation.childForeignKeyField(),
                relation.autoPopulate(),
                relation.autoDeleteWithParent()
        );
    }
}
