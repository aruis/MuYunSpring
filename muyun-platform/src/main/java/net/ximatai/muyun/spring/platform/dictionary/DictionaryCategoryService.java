package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
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
public class DictionaryCategoryService extends AbstractAbilityService<DictionaryCategory> implements
        SoftDeleteAbility<DictionaryCategory>,
        EnableAbility<DictionaryCategory>,
        TreeAbility<DictionaryCategory>, QueryAbility<DictionaryCategory>
{
    public static final String MODULE_ALIAS = "platform.dictionary_category";

    public DictionaryCategoryService(BaseDao<DictionaryCategory, String> categoryDao) {
        super(MODULE_ALIAS, DictionaryCategory.class, categoryDao);
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("applicationAlias", QueryOperator.EQ, QueryOperator.IN).withTitle("所属应用"))
                .field(QueryField.of("alias", QueryOperator.EQ, QueryOperator.IN).withTitle("标识"))
                .field(QueryField.of("categoryKind", QueryOperator.EQ).withTitle("分类类型"))
                .field(QueryField.of("parentId", QueryOperator.EQ, QueryOperator.IN).withTitle("父ID"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("名称").withQuickSearch().withSortable())
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
                .defaultSort(Sort.asc("title"))
                .build();
    }
    @Override
    public void beforeInsert(DictionaryCategory category) {
        normalizeAndValidate(category);
    }

    @Override
    public void beforeUpdate(DictionaryCategory category) {
        normalizeAndValidate(category);
        validateImmutableIdentity(category);
    }

    @Override
    public Criteria sortScope(DictionaryCategory category) {
        return scopedTreeCriteria(category, "applicationAlias");
    }

    @Override
    public void validateSortScope(DictionaryCategory left, DictionaryCategory right) {
        validateTreeSortScopeByFields(left, right,
                "Dictionary category sort can only move records within the same application", "applicationAlias");
    }

    @Override
    public List<DictionaryCategory> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootCategories(applicationAlias)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<DictionaryCategory> rootCategories(String applicationAlias) {
        return children(applicationAlias, TreeAbility.ROOT_ID);
    }

    public List<DictionaryCategory> rootCategories() {
        return TreeAbility.super.children(TreeAbility.ROOT_ID);
    }

    public List<DictionaryCategory> children(String applicationAlias, String parentId) {
        return TreeAbility.super.children(applicationScope(PlatformNameRules.requireApplicationAlias(applicationAlias)), parentId);
    }

    public DictionaryCategory requireDictionaryCategory(String applicationAlias, String categoryAlias) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireAlias(categoryAlias);
        DictionaryCategory category = findOne(Criteria.of()
                        .eq("applicationAlias", validApplicationAlias)
                        .eq("alias", validCategoryAlias));
        if (category == null) {
            throw new PlatformException("Dictionary category requires existing category: " + validCategoryAlias);
        }
        if (category.getCategoryKind() != DictionaryCategoryKind.DICTIONARY) {
            throw new PlatformException("Dictionary items require DICTIONARY category: " + validCategoryAlias);
        }
        return category;
    }

    public DictionaryCategory requireDictionaryCategory(String categoryId) {
        String validCategoryId = requireText(categoryId, "dictionaryCategoryId");
        DictionaryCategory category = select(validCategoryId);
        if (category == null) {
            throw new PlatformException("Dictionary category requires existing category: " + validCategoryId);
        }
        if (category.getCategoryKind() != DictionaryCategoryKind.DICTIONARY) {
            throw new PlatformException("Dictionary items require DICTIONARY category: " + validCategoryId);
        }
        return category;
    }

    public DictionaryCategory requireEnabledDictionaryCategory(String applicationAlias, String categoryAlias) {
        DictionaryCategory category = requireDictionaryCategory(applicationAlias, categoryAlias);
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new PlatformException("Dictionary category is disabled: " + categoryAlias);
        }
        return category;
    }

    private void normalizeAndValidate(DictionaryCategory category) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(category.getApplicationAlias());
        String alias = requireAlias(category.getAlias());
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        if (category.getCategoryKind() == null) {
            category.setCategoryKind(DictionaryCategoryKind.DICTIONARY);
        }
        rejectDuplicate(category, Criteria.of()
                        .eq("applicationAlias", category.getApplicationAlias())
                        .eq("alias", category.getAlias()),
                "dictionaryCategoryAlias must be unique within application: " + category.getAlias());
        validateParentApplication(category);
    }

    private String requireAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "dictionaryCategoryAlias");
    }

    private void validateParentApplication(DictionaryCategory category) {
        validateTreePlacementInScope(category, applicationScope(category.getApplicationAlias()),
                "Dictionary category parent must belong to the same application");
    }

    private void validateImmutableIdentity(DictionaryCategory category) {
        DictionaryCategory existing = selectIncludingDeleted(category.getId());
        rejectChanged(existing, category, "Dictionary category application", DictionaryCategory::getApplicationAlias);
        rejectChanged(existing, category, "Dictionary category alias", DictionaryCategory::getAlias);
        rejectChanged(existing, category, "Dictionary category kind", DictionaryCategory::getCategoryKind);
    }

    private Criteria applicationScope(String applicationAlias) {
        return Criteria.of().eq("applicationAlias", applicationAlias);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " is required");
        }
        return value.trim();
    }
}
