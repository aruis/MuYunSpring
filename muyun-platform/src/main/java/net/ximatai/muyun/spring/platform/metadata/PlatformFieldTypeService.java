package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.stereotype.Service;

@Service
public class PlatformFieldTypeService extends AbstractAbilityService<PlatformFieldType> implements
        SoftDeleteAbility<PlatformFieldType>,
        EnableAbility<PlatformFieldType>,
        SortAbility<PlatformFieldType> {
    public static final String MODULE_ALIAS = "platform.fieldType";

    public PlatformFieldTypeService(BaseDao<PlatformFieldType, String> fieldTypeDao) {
        super(MODULE_ALIAS, PlatformFieldType.class, fieldTypeDao);
    }

    @Override
    public void beforePrepareInsert(PlatformFieldType fieldType) {
        if (fieldType.getId() == null || fieldType.getId().isBlank()) {
            fieldType.setId(PlatformNameRules.requireIdentifier(fieldType.getAlias(), "fieldTypeAlias"));
        }
    }

    @Override
    public void beforeInsert(PlatformFieldType fieldType) {
        normalizeAndValidate(fieldType);
    }

    @Override
    public void beforeUpdate(PlatformFieldType fieldType) {
        normalizeAndValidate(fieldType);
        PlatformFieldType existing = selectIncludingDeleted(fieldType.getId());
        rejectChanged(existing, fieldType, "Field type alias", PlatformFieldType::getAlias);
    }

    @Override
    public Criteria sortScope(PlatformFieldType fieldType) {
        return Criteria.of();
    }

    public PlatformFieldType requireFieldType(String alias) {
        String validAlias = PlatformNameRules.requireIdentifier(alias, "fieldTypeAlias");
        PlatformFieldType fieldType = findOne(Criteria.of().eq("alias", validAlias));
        if (fieldType == null) {
            throw new PlatformException("Field type requires existing type: " + validAlias);
        }
        return fieldType;
    }

    private void normalizeAndValidate(PlatformFieldType fieldType) {
        String alias = PlatformNameRules.requireIdentifier(fieldType.getAlias(), "fieldTypeAlias");
        fieldType.setAlias(alias);
        if (fieldType.getTitle() == null || fieldType.getTitle().isBlank()) {
            fieldType.setTitle(alias);
        }
        if (fieldType.getFieldType() == null) {
            fieldType.setFieldType(FieldType.STRING);
        }
        FieldShapeRules.validate(fieldType.getFieldType(), fieldType.getDefaultLength(),
                fieldType.getDefaultPrecision(), fieldType.getDefaultScale(), alias);
        normalizeQueryDefinition(fieldType);
        rejectDuplicate(fieldType, Criteria.of().eq("alias", alias),
                "fieldTypeAlias must be unique: " + alias);
    }

    private void normalizeQueryDefinition(PlatformFieldType fieldType) {
        if (fieldType.getDefaultQueryOperator() == null && (fieldType.getQueryOperators() == null
                || fieldType.getQueryOperators().isBlank())) {
            return;
        }
        if (fieldType.getDefaultQueryOperator() == null) {
            fieldType.setDefaultQueryOperator(DynamicQueryOperator.defaultOperator(fieldType.getFieldType()));
        }
        if (fieldType.getQueryOperators() == null || fieldType.getQueryOperators().isBlank()) {
            fieldType.setQueryOperators(DynamicQueryOperator.format(DynamicQueryOperator.defaultOperators(fieldType.getFieldType())));
        }
        fieldType.queryDefinition();
    }
}
