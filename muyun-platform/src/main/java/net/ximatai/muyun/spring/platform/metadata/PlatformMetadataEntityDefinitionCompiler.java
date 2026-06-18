package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
public class PlatformMetadataEntityDefinitionCompiler {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final MetadataFieldDefinitionCompiler fieldDefinitionCompiler;

    public PlatformMetadataEntityDefinitionCompiler(MetadataService metadataService,
                                                    MetadataFieldService fieldService,
                                                    MetadataFieldDefinitionCompiler fieldDefinitionCompiler) {
        this.metadataService = Objects.requireNonNull(metadataService, "metadataService must not be null");
        this.fieldService = Objects.requireNonNull(fieldService, "fieldService must not be null");
        this.fieldDefinitionCompiler = Objects.requireNonNull(fieldDefinitionCompiler,
                "fieldDefinitionCompiler must not be null");
    }

    public EntityDefinition compile(String metadataId) {
        Metadata metadata = metadataId == null || metadataId.isBlank() ? null : metadataService.select(metadataId);
        if (metadata == null) {
            throw new PlatformException("Metadata schema ensure requires existing metadata: " + metadataId);
        }
        return compile(metadata);
    }

    public EntityDefinition compile(Metadata metadata) {
        if (metadata == null || metadata.getId() == null || metadata.getId().isBlank()) {
            throw new PlatformException("Metadata schema ensure requires persisted metadata");
        }
        List<FieldDefinition> fields = fields(metadata.getId());
        return new EntityDefinition(
                metadata.getAlias(),
                metadata.getSchemaName(),
                metadata.getTableName(),
                metadata.getTitle(),
                fields,
                capabilities(metadata, fields),
                List.of()
        );
    }

    private List<FieldDefinition> fields(String metadataId) {
        return fieldService.list(
                        Criteria.of().eq("metadataId", metadataId),
                        ALL,
                        Sort.asc(PlatformAbilityFields.SORT_FIELD)
                )
                .stream()
                .map(fieldDefinitionCompiler::compile)
                .toList();
    }

    private EnumSet<EntityCapability> capabilities(Metadata metadata, List<FieldDefinition> fields) {
        EnumSet<EntityCapability> capabilities = EnumSet.of(EntityCapability.CRUD);
        for (FieldDefinition field : fields) {
            if (PlatformAbilityFields.TREE_PARENT_FIELD.equals(field.fieldName())) {
                capabilities.add(EntityCapability.TREE);
            }
            if (field.isSortable()) {
                capabilities.add(EntityCapability.SORT);
            }
            if (field.isTitle()) {
                capabilities.add(EntityCapability.REFERENCE);
            }
            if (PlatformAbilityFields.ENABLED_FIELD.equals(field.fieldName())
                    || PlatformAbilityFields.ENABLED_COLUMN.equals(field.columnName())) {
                capabilities.add(EntityCapability.ENABLE);
            }
            if (isApprovalField(field)) {
                capabilities.add(EntityCapability.APPROVAL);
            }
        }
        if (Boolean.TRUE.equals(metadata.getDataScopeEnabled())) {
            capabilities.add(EntityCapability.DATA_SCOPE);
        }
        return capabilities;
    }

    private boolean isApprovalField(FieldDefinition field) {
        return PlatformAbilityFields.APPROVAL_INSTANCE_FIELD.equals(field.fieldName())
                || PlatformAbilityFields.APPROVAL_STATUS_FIELD.equals(field.fieldName())
                || PlatformAbilityFields.APPROVAL_SUBMITTED_BY_FIELD.equals(field.fieldName())
                || PlatformAbilityFields.APPROVAL_SUBMITTED_AT_FIELD.equals(field.fieldName())
                || PlatformAbilityFields.APPROVAL_COMPLETED_AT_FIELD.equals(field.fieldName())
                || PlatformAbilityFields.APPROVAL_INSTANCE_COLUMN.equals(field.columnName())
                || PlatformAbilityFields.APPROVAL_STATUS_COLUMN.equals(field.columnName())
                || PlatformAbilityFields.APPROVAL_SUBMITTED_BY_COLUMN.equals(field.columnName())
                || PlatformAbilityFields.APPROVAL_SUBMITTED_AT_COLUMN.equals(field.columnName())
                || PlatformAbilityFields.APPROVAL_COMPLETED_AT_COLUMN.equals(field.columnName());
    }
}
