package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class PlatformFieldTypeService extends AbstractAbilityService<PlatformFieldType> implements
        SoftDeleteAbility<PlatformFieldType>,
        EnableAbility<PlatformFieldType>,
        SortAbility<PlatformFieldType> {
    public static final String MODULE_ALIAS = "platform.field_type";
    private final BaseDao<PlatformFieldUiType, String> fieldUiTypeDao;

    public PlatformFieldTypeService(BaseDao<PlatformFieldType, String> fieldTypeDao) {
        this(fieldTypeDao, null);
    }

    @Autowired
    public PlatformFieldTypeService(BaseDao<PlatformFieldType, String> fieldTypeDao,
                                    BaseDao<PlatformFieldUiType, String> fieldUiTypeDao) {
        super(MODULE_ALIAS, PlatformFieldType.class, fieldTypeDao);
        this.fieldUiTypeDao = fieldUiTypeDao;
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
        normalizeUiTypeAliases(fieldType);
        rejectDuplicate(fieldType, Criteria.of().eq("alias", alias),
                "fieldTypeAlias must be unique: " + alias);
    }

    private void normalizeQueryDefinition(PlatformFieldType fieldType) {
        if (fieldType.getDefaultQueryOperator() == null && (fieldType.getQueryOperators() == null
                || fieldType.getQueryOperators().isEmpty())) {
            return;
        }
        if (fieldType.getDefaultQueryOperator() == null) {
            fieldType.setDefaultQueryOperator(DynamicQueryOperator.defaultOperator(fieldType.getFieldType()));
        }
        if (fieldType.getQueryOperators() == null || fieldType.getQueryOperators().isEmpty()) {
            fieldType.setQueryOperators(DynamicQueryOperator.names(DynamicQueryOperator.defaultOperators(fieldType.getFieldType())));
        } else {
            fieldType.setQueryOperators(DynamicQueryOperator.names(DynamicQueryOperator.parseNames(fieldType.getQueryOperators())));
        }
        fieldType.queryDefinition();
    }

    private void normalizeUiTypeAliases(PlatformFieldType fieldType) {
        if (fieldType.getDefaultUiTypeAlias() != null && !fieldType.getDefaultUiTypeAlias().isBlank()) {
            fieldType.setDefaultUiTypeAlias(PlatformNameRules.requireIdentifier(
                    fieldType.getDefaultUiTypeAlias().trim(), "defaultUiTypeAlias"));
            requireFieldUiType(fieldType.getDefaultUiTypeAlias());
        }
        if (fieldType.getUiTypeAliases() == null || fieldType.getUiTypeAliases().isEmpty()) {
            return;
        }
        Set<String> aliases = new LinkedHashSet<>();
        for (String alias : fieldType.getUiTypeAliases()) {
            String validAlias = PlatformNameRules.requireIdentifier(alias == null ? null : alias.trim(), "uiTypeAlias");
            requireFieldUiType(validAlias);
            aliases.add(validAlias);
        }
        if (fieldType.getDefaultUiTypeAlias() != null && !aliases.contains(fieldType.getDefaultUiTypeAlias())) {
            throw new PlatformException("default UI type must be included in allowed UI types: "
                    + fieldType.getDefaultUiTypeAlias());
        }
        fieldType.setUiTypeAliases(aliases);
    }

    private void requireFieldUiType(String alias) {
        if (fieldUiTypeDao != null
                && fieldUiTypeDao.list(Criteria.of().eq("alias", alias), new PageRequest(0, 1)).isEmpty()) {
            throw new PlatformException("Field type UI alias requires existing UI type: " + alias);
        }
    }
}
