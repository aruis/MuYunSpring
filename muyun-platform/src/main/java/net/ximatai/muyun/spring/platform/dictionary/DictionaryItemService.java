package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DictionaryItemService extends AbstractAbilityService<DictionaryItem> implements
        SoftDeleteAbility<DictionaryItem>,
        EnableAbility<DictionaryItem>,
        TreeAbility<DictionaryItem> {
    public static final String MODULE_ALIAS = "platform.dictionary_item";

    private final DictionaryCategoryService categoryService;

    public DictionaryItemService(BaseDao<DictionaryItem, String> itemDao,
                                 DictionaryCategoryService categoryService) {
        super(MODULE_ALIAS, DictionaryItem.class, itemDao);
        this.categoryService = categoryService;
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
    public Criteria sortScope(DictionaryItem item) {
        return scopedTreeCriteria(categoryScope(item.getApplicationAlias(), item.getCategoryAlias()), item.getParentId());
    }

    @Override
    public void validateSortScope(DictionaryItem left, DictionaryItem right) {
        if (!Objects.equals(left.getApplicationAlias(), right.getApplicationAlias())
                || !Objects.equals(left.getCategoryAlias(), right.getCategoryAlias())) {
            throw new PlatformException("Dictionary item sort can only move records within the same category");
        }
        TreeAbility.super.validateSortScope(left, right);
    }

    @Override
    public List<DictionaryItem> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootItems(applicationAlias, categoryAlias)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<DictionaryItem> rootItems(String applicationAlias, String categoryAlias) {
        return children(applicationAlias, categoryAlias, TreeAbility.ROOT_ID);
    }

    public List<DictionaryItem> children(String applicationAlias, String categoryAlias, String parentId) {
        return TreeAbility.super.children(categoryScope(
                PlatformNameRules.requireApplicationAlias(applicationAlias),
                requireCode(categoryAlias, "dictionaryCategoryAlias")), parentId);
    }

    public DictionaryItem resolveItem(String applicationAlias, String categoryAlias, String code) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireCode(categoryAlias, "dictionaryCategoryAlias");
        String validCode = requireCode(code, "dictionaryItemCode");
        return findOne(Criteria.of()
                        .eq("applicationAlias", validApplicationAlias)
                        .eq("categoryAlias", validCategoryAlias)
                        .eq("code", validCode));
    }

    private void normalizeAndValidate(DictionaryItem item) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(item.getApplicationAlias());
        String categoryAlias = requireCode(item.getCategoryAlias(), "dictionaryCategoryAlias");
        DictionaryCategory category = categoryService.requireDictionaryCategory(applicationAlias, categoryAlias);
        String code = requireCode(item.getCode(), "dictionaryItemCode");
        item.setApplicationAlias(category.getApplicationAlias());
        item.setCategoryAlias(category.getAlias());
        item.setCode(code);
        rejectDuplicate(item, Criteria.of()
                        .eq("applicationAlias", item.getApplicationAlias())
                        .eq("categoryAlias", item.getCategoryAlias())
                        .eq("code", item.getCode()),
                "dictionary item code must be unique within category: " + item.getCode());
        validateParentCategory(item);
    }

    private String requireCode(String value, String name) {
        return PlatformNameRules.requireCode(value, name);
    }

    private void validateParentCategory(DictionaryItem item) {
        validateTreePlacementInScope(item, categoryScope(item.getApplicationAlias(), item.getCategoryAlias()),
                "Dictionary item parent must belong to the same category");
    }

    private void validateImmutableIdentity(DictionaryItem item) {
        DictionaryItem existing = selectIncludingDeleted(item.getId());
        rejectChanged(existing, item, "Dictionary item application", DictionaryItem::getApplicationAlias);
        rejectChanged(existing, item, "Dictionary item category", DictionaryItem::getCategoryAlias);
        rejectChanged(existing, item, "Dictionary item code", DictionaryItem::getCode);
    }

    private Criteria categoryScope(String applicationAlias, String categoryAlias) {
        return Criteria.of()
                .eq("applicationAlias", applicationAlias)
                .eq("categoryAlias", categoryAlias);
    }
}
