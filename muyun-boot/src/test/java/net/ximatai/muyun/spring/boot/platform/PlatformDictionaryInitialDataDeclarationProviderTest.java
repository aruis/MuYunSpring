package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryKind;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformDictionaryInitialDataDeclarationProviderTest {
    private final TestMemoryDao<DictionaryCategory> categoryDao = new TestMemoryDao<>();
    private final TestMemoryDao<DictionaryItem> itemDao = new TestMemoryDao<>();
    private final DictionaryCategoryService categoryService = new DictionaryCategoryService(categoryDao);
    private final DictionaryItemService itemService = new DictionaryItemService(itemDao, categoryService);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldRegisterIamGenderDictionaryOnStartup() {
        initializePlatformDictionaries();

        DictionaryCategory category = categoryService.requireDictionaryCategory("iam", "gender");
        assertThat(category).satisfies(value -> {
            assertThat(value.getId()).isEqualTo("platform.dict.iam.gender");
            assertThat(value.getTitle()).isEqualTo("性别");
            assertThat(value.getCategoryKind()).isEqualTo(DictionaryCategoryKind.DICTIONARY);
            assertThat(value.getEnabled()).isTrue();
            assertThat(value.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
            assertThat(value.getSortOrder()).isEqualTo(10);
        });
        assertThat(itemService.rootItems("iam", "gender"))
                .extracting(DictionaryItem::getCode, DictionaryItem::getTitle, DictionaryItem::getEnabled,
                        DictionaryItem::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("1", "男", Boolean.TRUE, 10),
                        org.assertj.core.groups.Tuple.tuple("2", "女", Boolean.TRUE, 20)
                );
    }

    @Test
    void shouldKeepRequiredDictionariesIdempotent() {
        initializePlatformDictionaries();
        initializePlatformDictionaries();

        assertThat(categoryDao.list(Criteria.of()
                .eq("applicationAlias", "iam")
                .eq("alias", "gender"))).hasSize(1);
        DictionaryCategory category = categoryService.requireDictionaryCategory("iam", "gender");
        assertThat(itemDao.list(Criteria.of().eq("categoryId", category.getId()))).hasSize(2);
    }

    @Test
    void shouldPreserveExistingDictionaryValuesAndFillMissingItems() {
        String existingCategoryId = categoryService.insert(
                dictionaryCategory("existing.iam.gender", "iam", "gender", "人员性别", 99));
        itemService.insert(dictionaryItem("existing.iam.gender.1", existingCategoryId, "1", "男性", 99));

        initializePlatformDictionaries();

        DictionaryCategory category = categoryService.requireDictionaryCategory("iam", "gender");
        assertThat(category).satisfies(value -> {
            assertThat(value.getId()).isEqualTo(existingCategoryId);
            assertThat(value.getTitle()).isEqualTo("人员性别");
            assertThat(value.getSortOrder()).isEqualTo(99);
        });
        assertThat(itemService.rootItems("iam", "gender"))
                .extracting(DictionaryItem::getCode, DictionaryItem::getTitle, DictionaryItem::getSortOrder)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("1", "男性", 99),
                        org.assertj.core.groups.Tuple.tuple("2", "女", 20)
                );
    }

    private void initializePlatformDictionaries() {
        new InitialDataExecutor(List.of(), List.of(
                new PlatformDictionaryInitialDataDeclarationProvider(
                        new DictionaryInitialDataDeclarations(categoryService, itemService))
        )).initializeAll();
    }

    private DictionaryCategory dictionaryCategory(String id,
                                                  String applicationAlias,
                                                  String alias,
                                                  String title,
                                                  int sortOrder) {
        DictionaryCategory category = new DictionaryCategory();
        category.setId(id);
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setCategoryKind(DictionaryCategoryKind.DICTIONARY);
        category.setParentId(TreeAbility.ROOT_ID);
        category.setTitle(title);
        category.setEnabled(Boolean.TRUE);
        category.setSortOrder(sortOrder);
        return category;
    }

    private DictionaryItem dictionaryItem(String id,
                                          String categoryId,
                                          String code,
                                          String title,
                                          int sortOrder) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        item.setCategoryAlias("gender");
        item.setCode(code);
        item.setParentId(TreeAbility.ROOT_ID);
        item.setTitle(title);
        item.setEnabled(Boolean.TRUE);
        item.setSortOrder(sortOrder);
        return item;
    }
}
