package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;
@Service
public class DictionaryItemService extends AbstractAbilityService<DictionaryItem> implements
        SoftDeleteAbility<DictionaryItem>,
        EnableAbility<DictionaryItem>,
        TreeAbility<DictionaryItem>,
        QueryAbility<DictionaryItem> {
    public static final String MODULE_ALIAS = "platform.dictionary_item";

    private final DictionaryCategoryService categoryService;

    public DictionaryItemService(BaseDao<DictionaryItem, String> itemDao,
                                 DictionaryCategoryService categoryService) {
        super(MODULE_ALIAS, DictionaryItem.class, itemDao);
        this.categoryService = categoryService;
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptors.fromModel(MODULE_ALIAS, DictionaryItem.class, java.util.List.of("id", "categoryId", "categoryAlias", "code", "parentId", "title", "enabled", "sortOrder", "createdAt", "updatedAt"),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"),
                net.ximatai.muyun.database.core.orm.Sort.asc("title"));
    }

    @Override
    public void beforeInsert(DictionaryItem item) {
        normalizeAndValidate(item);
    }

    @Override
    public void beforeUpdate(DictionaryItem item) {
        normalizeAndValidate(item);
        validateImmutableIdentity(item);
    }

    @Override
    public List<DictionaryItem> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootItems(applicationAlias, categoryAlias)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<DictionaryItem> rootItems(String applicationAlias, String categoryAlias) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
        return rootItems(category.getId());
    }

    public List<DictionaryItem> rootItems(String categoryId) {
        return children(categoryId, TreeAbility.ROOT_ID);
    }

    public List<DictionaryItem> children(String applicationAlias, String categoryAlias, String parentId) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
        return children(category.getId(), parentId);
    }

    public List<DictionaryItem> children(String categoryId, String parentId) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(categoryId);
        return TreeAbility.super.children(categoryScope(category.getId()), parentId);
    }

    public DictionaryCategory category(String categoryId) {
        return categoryService.requireDictionaryCategory(categoryId);
    }

    public DictionaryCategory category(String applicationAlias, String categoryAlias) {
        return categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
    }

    public DictionaryItem resolveItem(String applicationAlias, String categoryAlias, String code) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
        return resolveItem(category.getId(), code);
    }

    public DictionaryItem resolveItem(String categoryId, String code) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(categoryId);
        String validCode = requireCode(code, "dictionaryItemCode");
        return findOne(Criteria.of()
                        .eq("categoryId", category.getId())
                        .eq("code", validCode));
    }

    public DictionaryItem resolveEnabledItem(String applicationAlias, String categoryAlias, String code) {
        DictionaryCategory category = categoryService.requireEnabledDictionaryCategory(applicationAlias, categoryAlias);
        return resolveEnabledItem(category.getId(), code);
    }

    public DictionaryItem resolveEnabledItem(String categoryId, String code) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(categoryId);
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new PlatformException("Dictionary category is disabled: " + category.getAlias());
        }
        String validCode = requireCode(code, "dictionaryItemCode");
        return findOne(Criteria.of()
                .eq("categoryId", category.getId())
                .eq("code", validCode)
                .eq("enabled", Boolean.TRUE));
    }

    public List<DictionaryItem> listItems(String applicationAlias, String categoryAlias, boolean enabledOnly) {
        DictionaryCategory category = enabledOnly
                ? categoryService.requireEnabledDictionaryCategory(applicationAlias, categoryAlias)
                : categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
        return listItems(category.getId(), enabledOnly);
    }

    public List<DictionaryItem> listItems(String categoryId, boolean enabledOnly) {
        DictionaryCategory category = categoryService.requireDictionaryCategory(categoryId);
        if (enabledOnly && !Boolean.TRUE.equals(category.getEnabled())) {
            throw new PlatformException("Dictionary category is disabled: " + category.getAlias());
        }
        Criteria criteria = categoryScope(category.getId());
        if (enabledOnly) {
            criteria.eq("enabled", Boolean.TRUE);
        }
        return list(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private void normalizeAndValidate(DictionaryItem item) {
        DictionaryCategory category = category(item);
        String code = requireCode(item.getCode(), "dictionaryItemCode");
        item.setCategoryId(category.getId());
        item.setCategoryAlias(category.getAlias());
        item.setCode(code);
        rejectDuplicate(item, Criteria.of()
                        .eq("categoryId", item.getCategoryId())
                        .eq("code", item.getCode()),
                "dictionary item code must be unique within category: " + item.getCode());
        rejectDuplicate(item, Criteria.of()
                        .eq("categoryId", item.getCategoryId())
                        .eq("title", item.getTitle()),
                "dictionary item title must be unique within category: " + item.getTitle());
        validateParentCategory(item);
    }

    private DictionaryCategory category(DictionaryItem item) {
        if (item.getCategoryId() != null && !item.getCategoryId().isBlank()) {
            return categoryService.requireDictionaryCategory(item.getCategoryId());
        }
        throw new PlatformException("Dictionary item requires categoryId");
    }

    private String requireCode(String value, String name) {
        return PlatformNameRules.requireCode(value, name);
    }

    private void validateParentCategory(DictionaryItem item) {
        validateTreePlacementInScope(item, categoryScope(item.getCategoryId()),
                "Dictionary item parent must belong to the same category");
    }

    private void validateImmutableIdentity(DictionaryItem item) {
        DictionaryItem existing = selectIncludingDeleted(item.getId());
        rejectChanged(existing, item, "Dictionary item category", DictionaryItem::getCategoryId);
        rejectChanged(existing, item, "Dictionary item code", DictionaryItem::getCode);
    }

    private Criteria categoryScope(String categoryId) {
        return Criteria.of().eq("categoryId", categoryId);
    }
}
