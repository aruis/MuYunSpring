package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldProtectionConfigService extends AbstractAbilityService<MetadataFieldProtectionConfig> implements
        SoftDeleteAbility<MetadataFieldProtectionConfig> {
    public static final String MODULE_ALIAS = "platform.metadata_field_protection_config";

    private final MetadataFieldService fieldService;
    private final PlatformFieldTypeService fieldTypeService;
    private final BaseDao<MetadataFieldConfig, String> fieldConfigDao;

    public MetadataFieldProtectionConfigService(BaseDao<MetadataFieldProtectionConfig, String> configDao,
                                                MetadataFieldService fieldService,
                                                PlatformFieldTypeService fieldTypeService) {
        this(configDao, fieldService, fieldTypeService, null);
    }

    @Autowired
    public MetadataFieldProtectionConfigService(BaseDao<MetadataFieldProtectionConfig, String> configDao,
                                                MetadataFieldService fieldService,
                                                PlatformFieldTypeService fieldTypeService,
                                                BaseDao<MetadataFieldConfig, String> fieldConfigDao) {
        super(MODULE_ALIAS, MetadataFieldProtectionConfig.class, configDao);
        this.fieldService = fieldService;
        this.fieldTypeService = fieldTypeService;
        this.fieldConfigDao = fieldConfigDao;
    }

    @Override
    public void beforeInsert(MetadataFieldProtectionConfig config) {
        normalizeAndValidate(config);
    }

    @Override
    public void beforeUpdate(MetadataFieldProtectionConfig config) {
        normalizeAndValidate(config);
    }

    public MetadataFieldProtectionConfig findByMetadataFieldId(String metadataFieldId) {
        if (metadataFieldId == null || metadataFieldId.isBlank()) {
            return null;
        }
        return findOne(Criteria.of().eq("metadataFieldId", metadataFieldId));
    }

    public FieldProtectionDefinition definition(String metadataFieldId) {
        MetadataFieldProtectionConfig config = findByMetadataFieldId(metadataFieldId);
        return config == null ? FieldProtectionDefinition.NONE : config.definition();
    }

    private void normalizeAndValidate(MetadataFieldProtectionConfig config) {
        MetadataField field = requireField(config.getMetadataFieldId());
        if (config.getEnabled() == null) {
            config.setEnabled(Boolean.TRUE);
        }
        if (config.getEncryptionMode() == null) {
            config.setEncryptionMode(FieldEncryptionMode.NONE);
        }
        if (config.getSignatureMode() == null) {
            config.setSignatureMode(FieldSignatureMode.NONE);
        }
        if (config.getMaskingPolicy() == null) {
            config.setMaskingPolicy(FieldMaskingPolicy.NONE);
        }
        FieldProtectionDefinition definition = config.definition();
        if (definition.enabled()) {
            validateFieldShape(field, definition);
            validateFieldConfig(field, definition);
        }
        rejectDuplicate(config, Criteria.of().eq("metadataFieldId", config.getMetadataFieldId()),
                "metadata field protection config must be unique: " + config.getMetadataFieldId());
    }

    private void validateFieldShape(MetadataField field, FieldProtectionDefinition definition) {
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        if (definition.hasStorageProtection()
                && fieldType.getFieldType() != FieldType.STRING
                && fieldType.getFieldType() != FieldType.TEXT) {
            throw new PlatformException("Field storage protection currently requires string field: " + field.getId());
        }
        if (definition.hasStorageProtection()
                && (Boolean.TRUE.equals(field.getUniqueField())
                || Boolean.TRUE.equals(field.getIndexed())
                || Boolean.TRUE.equals(field.getSortableField())
                || Boolean.TRUE.equals(field.getTitleField()))) {
            throw new PlatformException("Protected storage field cannot be unique, indexed, sortable or title field: "
                    + field.getId());
        }
    }

    private void validateFieldConfig(MetadataField field, FieldProtectionDefinition definition) {
        if (!definition.hasStorageProtection()) {
            return;
        }
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        MetadataFieldConfig config = fieldConfig(field.getId());
        if (config == null) {
            if (fieldType.queryDefinition().queryable()) {
                throw new PlatformException("Protected storage field cannot be queryable: " + field.getId());
            }
            return;
        }
        if (config.queryDefinition(fieldType).queryable()) {
            throw new PlatformException("Protected storage field cannot be queryable: " + field.getId());
        }
    }

    private MetadataFieldConfig fieldConfig(String metadataFieldId) {
        if (fieldConfigDao == null) {
            return null;
        }
        return fieldConfigDao.query(Criteria.of()
                        .eq("metadataFieldId", metadataFieldId)
                        .isNull("relationId"),
                net.ximatai.muyun.database.core.orm.PageRequest.of(1, 1)).stream()
                .findFirst()
                .orElse(null);
    }

    private MetadataField requireField(String metadataFieldId) {
        MetadataField field = metadataFieldId == null || metadataFieldId.isBlank() ? null : fieldService.select(metadataFieldId);
        if (field == null) {
            throw new PlatformException("Field protection config requires existing metadata field: " + metadataFieldId);
        }
        return field;
    }
}
