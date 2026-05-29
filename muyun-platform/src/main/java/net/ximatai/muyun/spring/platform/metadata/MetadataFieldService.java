package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MetadataFieldService extends AbstractAbilityService<MetadataField> implements
        SoftDeleteAbility<MetadataField>,
        EnableAbility<MetadataField>,
        SortAbility<MetadataField> {
    public static final String MODULE_ALIAS = "platform.metadataField";

    private final MetadataService metadataService;
    private final DictionaryCategoryService categoryService;

    public MetadataFieldService(BaseDao<MetadataField, String> fieldDao,
                                MetadataService metadataService,
                                DictionaryCategoryService categoryService) {
        super(MODULE_ALIAS, MetadataField.class, fieldDao);
        this.metadataService = metadataService;
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService must not be null");
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
        normalizeDictionaryBinding(field);
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

    private void normalizeDictionaryBinding(MetadataField field) {
        boolean hasCategory = field.getDictionaryCategoryAlias() != null && !field.getDictionaryCategoryAlias().isBlank();
        boolean hasApplication = field.getDictionaryApplicationAlias() != null && !field.getDictionaryApplicationAlias().isBlank();
        if (!hasCategory && !hasApplication) {
            field.setDictionaryApplicationAlias(null);
            field.setDictionaryCategoryAlias(null);
            return;
        }
        if (!hasCategory) {
            throw new IllegalArgumentException("dictionaryCategoryAlias must not be blank");
        }
        String applicationAlias = hasApplication
                ? PlatformNameRules.requireApplicationAlias(field.getDictionaryApplicationAlias())
                : requireMetadata(field.getMetadataId()).getApplicationAlias();
        field.setDictionaryApplicationAlias(applicationAlias);
        field.setDictionaryCategoryAlias(PlatformNameRules.requireIdentifier(
                field.getDictionaryCategoryAlias(), "dictionaryCategoryAlias"));
        categoryService.requireDictionaryCategory(field.getDictionaryApplicationAlias(), field.getDictionaryCategoryAlias());
        if (field.getFieldType() != null && field.getFieldType() != FieldType.STRING && field.getFieldType() != FieldType.TEXT) {
            throw new IllegalArgumentException("dictionary binding requires string field");
        }
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
