package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionKind;

public record DynamicFieldCompanionDescriptor(
        String fieldName,
        String kind,
        String role,
        boolean requiredWhenOwnerPresent,
        boolean requiredWhenOwnerUpdated
) {
    public static DynamicFieldCompanionDescriptor from(FieldCompanionKind kind, FieldCompanionDefinition companion) {
        return new DynamicFieldCompanionDescriptor(
                companion.fieldName(),
                kind.name(),
                companion.role().name(),
                companion.requiredWhenOwnerPresent(),
                companion.requiredWhenOwnerUpdated()
        );
    }
}
