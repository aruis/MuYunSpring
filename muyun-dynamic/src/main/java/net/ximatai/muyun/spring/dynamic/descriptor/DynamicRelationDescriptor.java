package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import java.util.List;

public record DynamicRelationDescriptor(
        String code,
        String parentEntityAlias,
        String childEntityAlias,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean cascadeOnParentUnavailable
) {
    public static DynamicRelationDescriptor from(String moduleAlias,
                                                 EntityRelationDefinition relation,
                                                 List<EntityReferenceDefinition> references) {
        return new DynamicRelationDescriptor(
                relation.code(),
                relation.parentEntityAlias(),
                relation.childEntityAlias(),
                relation.childForeignKeyField(),
                relation.autoPopulate(),
                relation.cascadeOnParentUnavailable(moduleAlias, references)
        );
    }
}
