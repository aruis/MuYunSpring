package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldService extends AbstractAbilityService<MetadataField> implements
        SoftDeleteAbility<MetadataField>,
        EnableAbility<MetadataField>,
        SortAbility<MetadataField> {
    public static final String MODULE_ALIAS = "platform.metadataField";

    private final MetadataService metadataService;
    private final PlatformFieldTypeService fieldTypeService;

    public MetadataFieldService(BaseDao<MetadataField, String> fieldDao,
                                MetadataService metadataService,
                                PlatformFieldTypeService fieldTypeService) {
        super(MODULE_ALIAS, MetadataField.class, fieldDao);
        this.metadataService = metadataService;
        this.fieldTypeService = fieldTypeService;
    }

    @Override
    public void beforeInsert(MetadataField field) {
        normalizeAndValidate(field);
    }

    @Override
    public void beforeUpdate(MetadataField field) {
        normalizeAndValidate(field);
    }

    @Override
    public Criteria sortScope(MetadataField field) {
        return Criteria.of().eq("metadataId", field.getMetadataId());
    }

    @Override
    public void validateSortScope(MetadataField left, MetadataField right) {
        if (!java.util.Objects.equals(left.getMetadataId(), right.getMetadataId())) {
            throw new PlatformException("Metadata field sort can only move records within the same metadata");
        }
    }

    private void normalizeAndValidate(MetadataField field) {
        requireMetadata(field.getMetadataId());
        PlatformNameRules.requireFieldName(field.getFieldName(), "fieldName");
        PlatformNameRules.requireDatabaseName(field.getColumnName(), "columnName");
        field.setFieldTypeAlias(PlatformNameRules.requireIdentifier(field.getFieldTypeAlias(), "fieldTypeAlias"));
        fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        if (field.getRequired() == null) {
            field.setRequired(Boolean.FALSE);
        }
        if (field.getUniqueField() == null) {
            field.setUniqueField(Boolean.FALSE);
        }
        if (field.getIndexed() == null) {
            field.setIndexed(Boolean.FALSE);
        }
        if (field.getSortableField() == null) {
            field.setSortableField(Boolean.FALSE);
        }
        if (field.getTitleField() == null) {
            field.setTitleField(Boolean.FALSE);
        }
        rejectDuplicateField(field);
        rejectDuplicateSingleFlag(field);
    }

    private void rejectDuplicateField(MetadataField field) {
        rejectDuplicate(field, Criteria.of()
                        .eq("metadataId", field.getMetadataId())
                        .eq("fieldName", field.getFieldName()),
                "metadata fieldName must be unique: " + field.getFieldName());
        rejectDuplicate(field, Criteria.of()
                        .eq("metadataId", field.getMetadataId())
                        .eq("columnName", field.getColumnName()),
                "metadata columnName must be unique: " + field.getColumnName());
    }

    private void rejectDuplicateSingleFlag(MetadataField field) {
        if (Boolean.TRUE.equals(field.getTitleField()) && existsOtherInCurrentScope(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("titleField", Boolean.TRUE))) {
            throw new PlatformException("metadata can only have one title field: " + field.getMetadataId());
        }
        if (Boolean.TRUE.equals(field.getSortableField()) && existsOtherInCurrentScope(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("sortableField", Boolean.TRUE))) {
            throw new PlatformException("metadata can only have one sortable field: " + field.getMetadataId());
        }
    }

    private Metadata requireMetadata(String metadataId) {
        Metadata metadata = metadataId == null || metadataId.isBlank() ? null : metadataService.select(metadataId);
        if (metadata == null) {
            throw new PlatformException("Metadata field requires existing metadata: " + metadataId);
        }
        return metadata;
    }
}
