package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;
import net.ximatai.muyun.spring.common.option.DictionaryField;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DictionaryFieldInitialDataDeclarationProviderTest {
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
    void shouldRegisterDictionaryBaselineDeclaredByStaticField() {
        initialize(SubjectRecord.class);

        DictionaryCategory category = categoryService.requireDictionaryCategory("education", "teaching_subject");
        assertThat(category).satisfies(value -> {
            assertThat(value.getId()).startsWith("dict.").hasSizeLessThanOrEqualTo(32);
            assertThat(value.getTitle()).isEqualTo("教学学科");
            assertThat(value.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        });
        assertThat(itemService.rootItems("education", "teaching_subject"))
                .extracting(DictionaryItem::getCode, DictionaryItem::getTitle, DictionaryItem::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("mathematics", "数学", 10),
                        org.assertj.core.groups.Tuple.tuple("chinese", "语文", 20));
    }

    @Test
    void shouldPreserveExistingDictionaryAndFillMissingItems() {
        String categoryId = categoryService.insert(dictionaryCategory("custom.subject", "education", "teaching_subject",
                "自定义教学学科", 99));
        itemService.insert(dictionaryItem("custom.subject.math", categoryId, "mathematics", "数学（自定义）", 99));

        initialize(SubjectRecord.class);

        DictionaryCategory category = categoryService.requireDictionaryCategory("education", "teaching_subject");
        assertThat(category.getId()).isEqualTo(categoryId);
        assertThat(category.getTitle()).isEqualTo("自定义教学学科");
        assertThat(itemService.rootItems("education", "teaching_subject"))
                .extracting(DictionaryItem::getCode, DictionaryItem::getTitle)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("mathematics", "数学（自定义）"),
                        org.assertj.core.groups.Tuple.tuple("chinese", "语文"));
    }

    @Test
    void shouldRejectMultipleBaselinesForOneDictionarySource() {
        assertThatThrownBy(() -> initialize(SubjectRecord.class, DuplicateSubjectRecord.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiple dictionary baselines declared for source: education.teaching_subject");
    }

    @SafeVarargs
    private final void initialize(Class<? extends StandardTitledEntity>... modelClasses) {
        new InitialDataExecutor(List.of(), List.of(
                new DictionaryFieldInitialDataDeclarationProvider(
                        new DictionaryInitialDataDeclarations(categoryService, itemService), List.of(modelClasses))
        )).initializeAll();
    }

    private DictionaryCategory dictionaryCategory(String id, String applicationAlias, String alias,
                                                  String title, int sortOrder) {
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

    private DictionaryItem dictionaryItem(String id, String categoryId, String code, String title, int sortOrder) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        item.setCategoryAlias("teaching_subject");
        item.setCode(code);
        item.setParentId(TreeAbility.ROOT_ID);
        item.setTitle(title);
        item.setEnabled(Boolean.TRUE);
        item.setSortOrder(sortOrder);
        return item;
    }

    private static class SubjectRecord extends StandardTitledEntity {
        @DictionaryField(
                source = "education.teaching_subject",
                title = "教学学科",
                initialItems = {
                        @DictionaryField.InitialItem(code = "mathematics", title = "数学", sortOrder = 10),
                        @DictionaryField.InitialItem(code = "chinese", title = "语文", sortOrder = 20)
                }
        )
        private String subjectCode;

    }

    private static class DuplicateSubjectRecord extends StandardTitledEntity {
        @DictionaryField(source = "education.teaching_subject", title = "冲突学科")
        private String subjectCode;

    }
}
