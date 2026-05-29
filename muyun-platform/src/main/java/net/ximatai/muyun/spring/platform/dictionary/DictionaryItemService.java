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
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
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
        return Criteria.of()
                .eq("applicationAlias", item.getApplicationAlias())
                .eq("categoryAlias", item.getCategoryAlias())
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, item.getParentId());
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
            throw new PlatformException("Use rootItems(applicationAlias, categoryAlias) to resolve category-scoped root items");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<DictionaryItem> rootItems(String applicationAlias, String categoryAlias) {
        return children(applicationAlias, categoryAlias, TreeAbility.ROOT_ID);
    }

    public List<DictionaryItem> children(String applicationAlias, String categoryAlias, String parentId) {
        PlatformAliasRules.requireApplicationAlias(applicationAlias);
        requireCode(categoryAlias, "dictionaryCategoryAlias");
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!TreeAbility.ROOT_ID.equals(parentId)) {
            DictionaryItem parent = selectActiveRaw(parentId);
            if (parent == null
                    || !applicationAlias.equals(parent.getApplicationAlias())
                    || !categoryAlias.equals(parent.getCategoryAlias())) {
                return List.of();
            }
        }
        Criteria criteria = activeCriteria(Criteria.of()
                .eq("applicationAlias", applicationAlias)
                .eq("categoryAlias", categoryAlias)
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public DictionaryItem resolveItem(String applicationAlias, String categoryAlias, String code) {
        String validApplicationAlias = PlatformAliasRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireCode(categoryAlias, "dictionaryCategoryAlias");
        String validCode = requireCode(code, "dictionaryItemCode");
        return list(Criteria.of()
                        .eq("applicationAlias", validApplicationAlias)
                        .eq("categoryAlias", validCategoryAlias)
                        .eq("code", validCode),
                PageRequest.of(1, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void normalizeAndValidate(DictionaryItem item) {
        String applicationAlias = PlatformAliasRules.requireApplicationAlias(item.getApplicationAlias());
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
        if (!PlatformAliasRules.isIdentifier(value)) {
            throw new IllegalArgumentException("invalid " + name + ": " + value);
        }
        return value;
    }

    private void validateParentCategory(DictionaryItem item) {
        String parentId = item.getParentId();
        if (parentId == null || parentId.isBlank() || TreeAbility.ROOT_ID.equals(parentId)) {
            return;
        }
        DictionaryItem parent = select(parentId);
        if (parent == null) {
            return;
        }
        if (!item.getApplicationAlias().equals(parent.getApplicationAlias())
                || !item.getCategoryAlias().equals(parent.getCategoryAlias())) {
            throw new PlatformException("Dictionary item parent must belong to the same category");
        }
    }

    private void validateImmutableIdentity(DictionaryItem item) {
        DictionaryItem existing = selectIgnoreSoftDelete(item.getId());
        if (existing == null) {
            return;
        }
        rejectChanged("Dictionary item application", existing.getApplicationAlias(), item.getApplicationAlias());
        rejectChanged("Dictionary item category", existing.getCategoryAlias(), item.getCategoryAlias());
        rejectChanged("Dictionary item code", existing.getCode(), item.getCode());
    }
}
