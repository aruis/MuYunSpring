package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PlatformFieldUiTypeFieldMappingService extends AbstractAbilityService<PlatformFieldUiTypeFieldMapping> implements
        SoftDeleteAbility<PlatformFieldUiTypeFieldMapping>,
        SortAbility<PlatformFieldUiTypeFieldMapping> {
    public static final String MODULE_ALIAS = "platform.fieldUiTypeFieldMapping";

    private final PlatformFieldUiTypeService fieldUiTypeService;

    public PlatformFieldUiTypeFieldMappingService(BaseDao<PlatformFieldUiTypeFieldMapping, String> mappingDao,
                                                  PlatformFieldUiTypeService fieldUiTypeService) {
        super(MODULE_ALIAS, PlatformFieldUiTypeFieldMapping.class, mappingDao);
        this.fieldUiTypeService = fieldUiTypeService;
    }

    @Override
    public void beforeInsert(PlatformFieldUiTypeFieldMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public void beforeUpdate(PlatformFieldUiTypeFieldMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public Criteria sortScope(PlatformFieldUiTypeFieldMapping mapping) {
        return Criteria.of().eq("fieldUiTypeAlias", mapping.getFieldUiTypeAlias());
    }

    @Override
    public void validateSortScope(PlatformFieldUiTypeFieldMapping left, PlatformFieldUiTypeFieldMapping right) {
        if (!Objects.equals(left.getFieldUiTypeAlias(), right.getFieldUiTypeAlias())) {
            throw new PlatformException("Field UI type mapping sort can only move records within the same UI type");
        }
    }

    private void normalizeAndValidate(PlatformFieldUiTypeFieldMapping mapping) {
        mapping.setFieldUiTypeAlias(PlatformNameRules.requireIdentifier(
                mapping.getFieldUiTypeAlias(), "fieldUiTypeAlias"));
        mapping.setSourceKey(PlatformNameRules.requireFieldName(mapping.getSourceKey(), "sourceKey"));
        fieldUiTypeService.requireFieldUiType(mapping.getFieldUiTypeAlias());
        if (mapping.getTitle() == null || mapping.getTitle().isBlank()) {
            mapping.setTitle(mapping.getSourceKey());
        }
        rejectDuplicate(mapping, Criteria.of()
                        .eq("fieldUiTypeAlias", mapping.getFieldUiTypeAlias())
                        .eq("sourceKey", mapping.getSourceKey()),
                "field UI type mapping must be unique: " + mapping.getFieldUiTypeAlias()
                        + "." + mapping.getSourceKey());
    }
}
