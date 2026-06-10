package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class PlatformFieldUiTypeService extends AbstractAbilityService<PlatformFieldUiType> implements
        SoftDeleteAbility<PlatformFieldUiType>,
        EnableAbility<PlatformFieldUiType>,
        SortAbility<PlatformFieldUiType> {
    public static final String MODULE_ALIAS = "platform.fieldUiType";

    private final PlatformFieldTypeService fieldTypeService;

    public PlatformFieldUiTypeService(BaseDao<PlatformFieldUiType, String> fieldUiTypeDao,
                                      PlatformFieldTypeService fieldTypeService) {
        super(MODULE_ALIAS, PlatformFieldUiType.class, fieldUiTypeDao);
        this.fieldTypeService = fieldTypeService;
    }

    @Override
    public void beforePrepareInsert(PlatformFieldUiType fieldUiType) {
        if (fieldUiType.getId() == null || fieldUiType.getId().isBlank()) {
            fieldUiType.setId(PlatformNameRules.requireIdentifier(fieldUiType.getAlias(), "fieldUiTypeAlias"));
        }
    }

    @Override
    public void beforeInsert(PlatformFieldUiType fieldUiType) {
        normalizeAndValidate(fieldUiType);
    }

    @Override
    public void beforeUpdate(PlatformFieldUiType fieldUiType) {
        normalizeAndValidate(fieldUiType);
        PlatformFieldUiType existing = selectIncludingDeleted(fieldUiType.getId());
        rejectChanged(existing, fieldUiType, "Field UI type alias", PlatformFieldUiType::getAlias);
    }

    @Override
    public Criteria sortScope(PlatformFieldUiType fieldUiType) {
        return Criteria.of();
    }

    public PlatformFieldUiType requireFieldUiType(String alias) {
        String validAlias = PlatformNameRules.requireIdentifier(alias, "fieldUiTypeAlias");
        PlatformFieldUiType fieldUiType = findOne(Criteria.of().eq("alias", validAlias));
        if (fieldUiType == null) {
            throw new PlatformException("Field UI type requires existing type: " + validAlias);
        }
        return fieldUiType;
    }

    private void normalizeAndValidate(PlatformFieldUiType fieldUiType) {
        String alias = PlatformNameRules.requireIdentifier(fieldUiType.getAlias(), "fieldUiTypeAlias");
        fieldUiType.setAlias(alias);
        if (fieldUiType.getTitle() == null || fieldUiType.getTitle().isBlank()) {
            fieldUiType.setTitle(alias);
        }
        if (fieldUiType.getDefaultFieldTypeAlias() != null && !fieldUiType.getDefaultFieldTypeAlias().isBlank()) {
            fieldUiType.setDefaultFieldTypeAlias(PlatformNameRules.requireIdentifier(
                    fieldUiType.getDefaultFieldTypeAlias(), "defaultFieldTypeAlias"));
            fieldTypeService.requireFieldType(fieldUiType.getDefaultFieldTypeAlias());
        }
        rejectDuplicate(fieldUiType, Criteria.of().eq("alias", alias),
                "fieldUiTypeAlias must be unique: " + alias);
    }
}
