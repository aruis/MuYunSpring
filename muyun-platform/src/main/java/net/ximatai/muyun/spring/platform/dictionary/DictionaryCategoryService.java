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
public class DictionaryCategoryService extends AbstractAbilityService<DictionaryCategory> implements
        SoftDeleteAbility<DictionaryCategory>,
        EnableAbility<DictionaryCategory>,
        TreeAbility<DictionaryCategory> {
    public static final String MODULE_ALIAS = "platform.dictionary_category";

    public DictionaryCategoryService(BaseDao<DictionaryCategory, String> categoryDao) {
        super(MODULE_ALIAS, DictionaryCategory.class, categoryDao);
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
        return Criteria.of()
                .eq("applicationAlias", category.getApplicationAlias())
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, category.getParentId());
    }

    @Override
    public void validateSortScope(DictionaryCategory left, DictionaryCategory right) {
        if (!Objects.equals(left.getApplicationAlias(), right.getApplicationAlias())) {
            throw new PlatformException("Dictionary category sort can only move records within the same application");
        }
        TreeAbility.super.validateSortScope(left, right);
    }

    @Override
    public List<DictionaryCategory> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            throw new PlatformException("Use rootCategories(applicationAlias) to resolve application-scoped root categories");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<DictionaryCategory> rootCategories(String applicationAlias) {
        return children(applicationAlias, TreeAbility.ROOT_ID);
    }

    public List<DictionaryCategory> children(String applicationAlias, String parentId) {
        PlatformAliasRules.requireApplicationAlias(applicationAlias);
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!TreeAbility.ROOT_ID.equals(parentId)) {
            DictionaryCategory parent = selectActiveRaw(parentId);
            if (parent == null || !applicationAlias.equals(parent.getApplicationAlias())) {
                return List.of();
            }
        }
        Criteria criteria = activeCriteria(Criteria.of()
                .eq("applicationAlias", applicationAlias)
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public DictionaryCategory requireDictionaryCategory(String applicationAlias, String categoryAlias) {
        String validApplicationAlias = PlatformAliasRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = requireAlias(categoryAlias);
        DictionaryCategory category = list(Criteria.of()
                        .eq("applicationAlias", validApplicationAlias)
                        .eq("alias", validCategoryAlias),
                PageRequest.of(1, 1))
                .stream()
                .findFirst()
                .orElse(null);
        if (category == null) {
            throw new PlatformException("Dictionary category requires existing category: " + validCategoryAlias);
        }
        if (category.getCategoryKind() != DictionaryCategoryKind.DICTIONARY) {
            throw new PlatformException("Dictionary items require DICTIONARY category: " + validCategoryAlias);
        }
        return category;
    }

    private void normalizeAndValidate(DictionaryCategory category) {
        String applicationAlias = PlatformAliasRules.requireApplicationAlias(category.getApplicationAlias());
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
        if (!PlatformAliasRules.isIdentifier(alias)) {
            throw new IllegalArgumentException("invalid dictionaryCategoryAlias: " + alias);
        }
        return alias;
    }

    private void validateParentApplication(DictionaryCategory category) {
        String parentId = category.getParentId();
        if (parentId == null || parentId.isBlank() || TreeAbility.ROOT_ID.equals(parentId)) {
            return;
        }
        DictionaryCategory parent = select(parentId);
        if (parent == null) {
            return;
        }
        if (!category.getApplicationAlias().equals(parent.getApplicationAlias())) {
            throw new PlatformException("Dictionary category parent must belong to the same application");
        }
    }

    private void validateImmutableIdentity(DictionaryCategory category) {
        DictionaryCategory existing = selectIgnoreSoftDelete(category.getId());
        if (existing == null) {
            return;
        }
        rejectChanged("Dictionary category application", existing.getApplicationAlias(), category.getApplicationAlias());
        rejectChanged("Dictionary category alias", existing.getAlias(), category.getAlias());
        rejectChanged("Dictionary category kind", existing.getCategoryKind(), category.getCategoryKind());
    }
}
