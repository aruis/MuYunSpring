package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
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
import java.util.Objects;

@Service
public class PlatformFieldUiTypeFieldMappingService extends AbstractAbilityService<PlatformFieldUiTypeFieldMapping> implements
        SoftDeleteAbility<PlatformFieldUiTypeFieldMapping>,
        SortAbility<PlatformFieldUiTypeFieldMapping>, QueryAbility<PlatformFieldUiTypeFieldMapping>
{
    public static final String MODULE_ALIAS = "platform.field_ui_type_field_mapping";

    private final PlatformFieldUiTypeService fieldUiTypeService;

    public PlatformFieldUiTypeFieldMappingService(BaseDao<PlatformFieldUiTypeFieldMapping, String> mappingDao,
                                                  PlatformFieldUiTypeService fieldUiTypeService) {
        super(MODULE_ALIAS, PlatformFieldUiTypeFieldMapping.class, mappingDao);
        this.fieldUiTypeService = fieldUiTypeService;
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("fieldUiTypeAlias", QueryOperator.EQ, QueryOperator.IN).withTitle("字段UI类型"))
                .field(QueryField.of("sourceKey", QueryOperator.EQ).withTitle("源Key"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("名称").withQuickSearch().withSortable())
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("sourceKey"))
                .build();
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

    public List<PlatformFieldUiTypeFieldMapping> listByFieldUiTypeAliases(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("fieldUiTypeAlias", aliases),
                new net.ximatai.muyun.database.core.orm.PageRequest(0, Integer.MAX_VALUE),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
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
