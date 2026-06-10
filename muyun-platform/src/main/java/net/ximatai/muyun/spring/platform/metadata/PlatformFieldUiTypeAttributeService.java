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
public class PlatformFieldUiTypeAttributeService extends AbstractAbilityService<PlatformFieldUiTypeAttribute> implements
        SoftDeleteAbility<PlatformFieldUiTypeAttribute>,
        SortAbility<PlatformFieldUiTypeAttribute> {
    public static final String MODULE_ALIAS = "platform.fieldUiTypeAttribute";

    private final PlatformFieldUiTypeService fieldUiTypeService;
    private final PlatformFieldTypeService fieldTypeService;

    public PlatformFieldUiTypeAttributeService(BaseDao<PlatformFieldUiTypeAttribute, String> attributeDao,
                                               PlatformFieldUiTypeService fieldUiTypeService,
                                               PlatformFieldTypeService fieldTypeService) {
        super(MODULE_ALIAS, PlatformFieldUiTypeAttribute.class, attributeDao);
        this.fieldUiTypeService = fieldUiTypeService;
        this.fieldTypeService = fieldTypeService;
    }

    @Override
    public void beforeInsert(PlatformFieldUiTypeAttribute attribute) {
        normalizeAndValidate(attribute);
    }

    @Override
    public void beforeUpdate(PlatformFieldUiTypeAttribute attribute) {
        normalizeAndValidate(attribute);
    }

    @Override
    public Criteria sortScope(PlatformFieldUiTypeAttribute attribute) {
        return Criteria.of().eq("fieldUiTypeAlias", attribute.getFieldUiTypeAlias());
    }

    @Override
    public void validateSortScope(PlatformFieldUiTypeAttribute left, PlatformFieldUiTypeAttribute right) {
        if (!Objects.equals(left.getFieldUiTypeAlias(), right.getFieldUiTypeAlias())) {
            throw new PlatformException("Field UI type attribute sort can only move records within the same UI type");
        }
    }

    private void normalizeAndValidate(PlatformFieldUiTypeAttribute attribute) {
        attribute.setFieldUiTypeAlias(PlatformNameRules.requireIdentifier(
                attribute.getFieldUiTypeAlias(), "fieldUiTypeAlias"));
        attribute.setAttributeAlias(PlatformNameRules.requireFieldName(
                attribute.getAttributeAlias(), "attributeAlias"));
        fieldUiTypeService.requireFieldUiType(attribute.getFieldUiTypeAlias());
        if (attribute.getTitle() == null || attribute.getTitle().isBlank()) {
            attribute.setTitle(attribute.getAttributeAlias());
        }
        if (attribute.getValueFieldTypeAlias() != null && !attribute.getValueFieldTypeAlias().isBlank()) {
            attribute.setValueFieldTypeAlias(PlatformNameRules.requireIdentifier(
                    attribute.getValueFieldTypeAlias(), "valueFieldTypeAlias"));
            fieldTypeService.requireFieldType(attribute.getValueFieldTypeAlias());
        }
        rejectDuplicate(attribute, Criteria.of()
                        .eq("fieldUiTypeAlias", attribute.getFieldUiTypeAlias())
                        .eq("attributeAlias", attribute.getAttributeAlias()),
                "field UI type attribute must be unique: " + attribute.getFieldUiTypeAlias()
                        + "." + attribute.getAttributeAlias());
    }
}
