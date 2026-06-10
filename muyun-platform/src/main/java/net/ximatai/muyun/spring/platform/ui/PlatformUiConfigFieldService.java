package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PlatformUiConfigFieldService extends AbstractAbilityService<PlatformUiConfigField> implements
        SoftDeleteAbility<PlatformUiConfigField>,
        EnableAbility<PlatformUiConfigField>,
        SortAbility<PlatformUiConfigField> {
    public static final String MODULE_ALIAS = "platform.uiConfigField";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiSetService uiSetService;
    private final ModuleMetadataFieldService moduleFieldService;
    private final PlatformFieldTypeService fieldTypeService;
    private final PlatformFieldUiTypeService fieldUiTypeService;
    private final MetadataFieldService metadataFieldService;

    public PlatformUiConfigFieldService(BaseDao<PlatformUiConfigField, String> uiConfigFieldDao,
                                        PlatformUiConfigService uiConfigService,
                                        PlatformUiSetService uiSetService,
                                        ModuleMetadataFieldService moduleFieldService,
                                        PlatformFieldTypeService fieldTypeService,
                                        PlatformFieldUiTypeService fieldUiTypeService,
                                        MetadataFieldService metadataFieldService) {
        super(MODULE_ALIAS, PlatformUiConfigField.class, uiConfigFieldDao);
        this.uiConfigService = uiConfigService;
        this.uiSetService = uiSetService;
        this.moduleFieldService = moduleFieldService;
        this.fieldTypeService = fieldTypeService;
        this.fieldUiTypeService = fieldUiTypeService;
        this.metadataFieldService = metadataFieldService;
    }

    @Override
    public void beforeInsert(PlatformUiConfigField field) {
        normalizeAndValidate(field);
    }

    @Override
    public void beforeUpdate(PlatformUiConfigField field) {
        normalizeAndValidate(field);
        PlatformUiConfigField existing = selectIncludingDeleted(field.getId());
        rejectChanged(existing, field, "UI config field config", PlatformUiConfigField::getUiConfigId);
        rejectChanged(existing, field, "UI config field module field",
                PlatformUiConfigField::getModuleMetadataFieldId);
    }

    @Override
    public Criteria sortScope(PlatformUiConfigField field) {
        return Criteria.of().eq("uiConfigId", field.getUiConfigId());
    }

    @Override
    public void validateSortScope(PlatformUiConfigField left, PlatformUiConfigField right) {
        if (!Objects.equals(left.getUiConfigId(), right.getUiConfigId())) {
            throw new PlatformException("UI config field sort can only move records within the same UI config");
        }
    }

    public List<PlatformUiConfigField> listByUiConfigIds(List<String> uiConfigIds) {
        if (uiConfigIds == null || uiConfigIds.isEmpty()) {
            return List.of();
        }
        return list(enabledCriteria(Criteria.of().in("uiConfigId", uiConfigIds)),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private void normalizeAndValidate(PlatformUiConfigField field) {
        PlatformUiConfig uiConfig = uiConfigService.requireUiConfig(field.getUiConfigId());
        PlatformUiSet uiSet = uiSetService.requireUiSet(uiConfig.getUiSetId());
        ResolvedModuleMetadataField moduleField = moduleFieldService.resolve(field.getModuleMetadataFieldId());
        if (!Objects.equals(uiSet.getModuleAlias(), moduleField.moduleAlias())) {
            throw new PlatformException("UI config field requires module field in the same module: "
                    + uiSet.getModuleAlias() + "." + moduleField.moduleAlias());
        }
        normalizeUiType(field, moduleField);
        validateRequiredOverride(field, moduleField);
        if (field.getVisible() == null) {
            field.setVisible(Boolean.TRUE);
        }
        if (field.getReadOnly() == null) {
            field.setReadOnly(Boolean.FALSE);
        }
        if (field.getTitle() == null || field.getTitle().isBlank()) {
            field.setTitle(moduleField.fieldTitle());
        }
        rejectDuplicate(field, Criteria.of()
                        .eq("uiConfigId", uiConfig.getId())
                        .eq("moduleMetadataFieldId", moduleField.moduleMetadataFieldId()),
                "UI config field must be unique in UI config: "
                        + uiConfig.getId() + "." + moduleField.moduleMetadataFieldId());
        field.setUiConfigId(uiConfig.getId());
        field.setModuleMetadataFieldId(moduleField.moduleMetadataFieldId());
    }

    private void normalizeUiType(PlatformUiConfigField field, ResolvedModuleMetadataField moduleField) {
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(moduleField.fieldTypeAlias());
        String uiTypeAlias = field.getFieldUiTypeAlias();
        if (uiTypeAlias == null || uiTypeAlias.isBlank()) {
            uiTypeAlias = fieldType.getDefaultUiTypeAlias();
        }
        if (uiTypeAlias == null || uiTypeAlias.isBlank()) {
            throw new PlatformException("Field UI type is required and field type has no default UI type: "
                    + moduleField.fieldTypeAlias());
        }
        uiTypeAlias = PlatformNameRules.requireIdentifier(uiTypeAlias, "fieldUiTypeAlias");
        PlatformFieldUiType uiType = fieldUiTypeService.requireFieldUiType(uiTypeAlias);
        if (fieldType.getUiTypeAliases() != null && !fieldType.getUiTypeAliases().isEmpty()) {
            if (!fieldType.getUiTypeAliases().contains(uiTypeAlias)) {
                throw new PlatformException("Field UI type is not allowed by field type: "
                        + moduleField.fieldTypeAlias() + "." + uiTypeAlias);
            }
        } else if (uiType.getDefaultFieldTypeAlias() != null
                && !uiType.getDefaultFieldTypeAlias().isBlank()
                && !Objects.equals(uiType.getDefaultFieldTypeAlias(), moduleField.fieldTypeAlias())) {
            throw new PlatformException("Field UI type default field type mismatch: "
                    + uiTypeAlias + "." + moduleField.fieldTypeAlias());
        }
        field.setFieldUiTypeAlias(uiTypeAlias);
    }

    private void validateRequiredOverride(PlatformUiConfigField field, ResolvedModuleMetadataField moduleField) {
        if (!Boolean.FALSE.equals(field.getRequiredOverride())) {
            return;
        }
        MetadataField metadataField = metadataFieldService.select(moduleField.metadataFieldId());
        if (metadataField != null && Boolean.TRUE.equals(metadataField.getRequired())) {
            throw new PlatformException("UI config field cannot weaken required metadata field: "
                    + moduleField.fieldName());
        }
    }
}
