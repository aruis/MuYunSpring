package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformFieldUiTypeService extends AbstractAbilityService<PlatformFieldUiType> implements
        SoftDeleteAbility<PlatformFieldUiType>,
        EnableAbility<PlatformFieldUiType>,
        SortAbility<PlatformFieldUiType>, QueryAbility<PlatformFieldUiType>
{
    public static final String MODULE_ALIAS = "platform.field_ui_type";

    private final PlatformFieldTypeService fieldTypeService;

    public PlatformFieldUiTypeService(BaseDao<PlatformFieldUiType, String> fieldUiTypeDao,
                                      PlatformFieldTypeService fieldTypeService) {
        super(MODULE_ALIAS, PlatformFieldUiType.class, fieldUiTypeDao);
        this.fieldTypeService = fieldTypeService;
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("alias", QueryOperator.EQ, QueryOperator.IN).withTitle("标识"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("名称").withQuickSearch().withSortable())
                .field(QueryField.of("defaultFieldTypeAlias", QueryOperator.EQ).withTitle("默认字段类型"))
                .field(QueryField.of("controlType", QueryOperator.EQ).withTitle("控件类型"))
                .field(QueryField.of("icon", QueryOperator.EQ).withTitle("图标"))
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
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

    public List<PlatformFieldUiType> listEnabledByAliases(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return List.of();
        }
        return list(enabledCriteria(Criteria.of().in("alias", aliases)),
                new net.ximatai.muyun.database.core.orm.PageRequest(0, Integer.MAX_VALUE),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
    }

    public List<PlatformFieldUiType> listEnabledForDefaultFieldType(String fieldTypeAlias) {
        String validAlias = PlatformNameRules.requireIdentifier(fieldTypeAlias, "fieldTypeAlias");
        return list(enabledCriteria(Criteria.of().eq("defaultFieldTypeAlias", validAlias)),
                new net.ximatai.muyun.database.core.orm.PageRequest(0, Integer.MAX_VALUE),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
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
