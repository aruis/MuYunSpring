package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldService extends AbstractAbilityService<MetadataField> implements
        SoftDeleteAbility<MetadataField>,
        EnableAbility<MetadataField>,
        SortAbility<MetadataField> {
    public static final String MODULE_ALIAS = "platform.metadataField";

    private final MetadataService metadataService;

    public MetadataFieldService(BaseDao<MetadataField, String> fieldDao, MetadataService metadataService) {
        super(MODULE_ALIAS, MetadataField.class, fieldDao);
        this.metadataService = metadataService;
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
            throw new AbilityException("Metadata field sort can only move records within the same metadata");
        }
    }

    private void normalizeAndValidate(MetadataField field) {
        requireMetadata(field.getMetadataId());
        MetadataService.requireFieldName(field.getFieldName(), "fieldName");
        MetadataService.requireIdentifier(field.getColumnName(), "columnName");
        if (field.getFieldType() == null) {
            field.setFieldType(FieldType.STRING);
        }
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
        if (field.getFieldLength() != null && field.getFieldLength() <= 0) {
            throw new IllegalArgumentException("fieldLength must be positive");
        }
        if (field.getPrecision() != null && field.getPrecision() <= 0) {
            throw new IllegalArgumentException("precision must be positive");
        }
        if (field.getScale() != null && field.getScale() < 0) {
            throw new IllegalArgumentException("scale must not be negative");
        }
        if (field.getScale() != null && field.getPrecision() != null && field.getScale() > field.getPrecision()) {
            throw new IllegalArgumentException("scale must not exceed precision");
        }
        rejectDuplicateField(field);
        rejectDuplicateSingleFlag(field);
    }

    private void rejectDuplicateField(MetadataField field) {
        boolean duplicateFieldName = hasOther(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("fieldName", field.getFieldName()));
        if (duplicateFieldName) {
            throw new AbilityException("metadata fieldName must be unique: " + field.getFieldName());
        }
        boolean duplicateColumnName = hasOther(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("columnName", field.getColumnName()));
        if (duplicateColumnName) {
            throw new AbilityException("metadata columnName must be unique: " + field.getColumnName());
        }
    }

    private void rejectDuplicateSingleFlag(MetadataField field) {
        if (Boolean.TRUE.equals(field.getTitleField()) && hasOther(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("titleField", Boolean.TRUE))) {
            throw new AbilityException("metadata can only have one title field: " + field.getMetadataId());
        }
        if (Boolean.TRUE.equals(field.getSortableField()) && hasOther(field, Criteria.of()
                .eq("metadataId", field.getMetadataId())
                .eq("sortableField", Boolean.TRUE))) {
            throw new AbilityException("metadata can only have one sortable field: " + field.getMetadataId());
        }
    }

    private boolean hasOther(MetadataField field, Criteria criteria) {
        return list(criteria, PageRequest.of(1, Integer.MAX_VALUE)).stream()
                .anyMatch(existing -> !java.util.Objects.equals(existing.getId(), field.getId()));
    }

    private void requireMetadata(String metadataId) {
        if (metadataId == null || metadataId.isBlank() || metadataService.select(metadataId) == null) {
            throw new AbilityException("Metadata field requires existing metadata: " + metadataId);
        }
    }
}
