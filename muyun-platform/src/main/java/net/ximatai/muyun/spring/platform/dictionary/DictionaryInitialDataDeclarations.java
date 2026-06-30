package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DictionaryInitialDataDeclarations {
    private static final PageRequest ONE = PageRequest.of(1, 1);

    private final DictionaryCategoryService categoryService;
    private final DictionaryItemService itemService;

    public DictionaryInitialDataDeclarations(DictionaryCategoryService categoryService,
                                             DictionaryItemService itemService) {
        this.categoryService = categoryService;
        this.itemService = itemService;
    }

    public List<InitialDataDeclaration<?>> declare(DictionarySeed... seeds) {
        return declare(seeds == null ? List.of() : Arrays.asList(seeds));
    }

    public List<InitialDataDeclaration<?>> declare(List<DictionarySeed> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }
        List<InitialDataDeclaration<?>> declarations = new ArrayList<>();
        for (DictionarySeed seed : seeds) {
            if (seed == null) {
                continue;
            }
            DictionaryCategory existingCategory = dictionaryCategory(seed.applicationAlias(), seed.alias());
            String categoryId = existingCategory == null ? seed.id() : existingCategory.getId();
            declarations.add(dictionaryCategory(seed));
            for (DictionaryItemSeed item : seed.items()) {
                declarations.add(dictionaryItem(seed, categoryId, item));
            }
        }
        return declarations;
    }

    private InitialDataDeclaration<DictionaryCategory> dictionaryCategory(DictionarySeed seed) {
        DictionaryCategory desired = new DictionaryCategory();
        desired.setId(seed.id());
        desired.setApplicationAlias(seed.applicationAlias());
        desired.setAlias(seed.alias());
        desired.setCategoryKind(DictionaryCategoryKind.DICTIONARY);
        desired.setParentId(TreeAbility.ROOT_ID);
        desired.setTitle(seed.title());
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(seed.sortOrder());
        InitialDataRecord<DictionaryCategory> record = InitialDataRecord
                .of(seed.applicationAlias() + "." + seed.alias(), InitialDataPolicy.CREATE_IF_MISSING, desired)
                .identity(
                        InitialDataField.of("applicationAlias", DictionaryCategory::getApplicationAlias,
                                DictionaryCategory::setApplicationAlias),
                        InitialDataField.of("alias", DictionaryCategory::getAlias,
                                DictionaryCategory::setAlias),
                        InitialDataField.of("categoryKind", DictionaryCategory::getCategoryKind,
                                DictionaryCategory::setCategoryKind)
                );
        return InitialDataDeclaration.of(
                record,
                () -> dictionaryCategory(seed.applicationAlias(), seed.alias()),
                categoryService::insert,
                categoryService::update
        );
    }

    private InitialDataDeclaration<DictionaryItem> dictionaryItem(DictionarySeed seed,
                                                                  String categoryId,
                                                                  DictionaryItemSeed item) {
        DictionaryItem desired = new DictionaryItem();
        desired.setId(itemId(seed, item));
        desired.setCategoryId(categoryId);
        desired.setCategoryAlias(seed.alias());
        desired.setCode(item.code());
        desired.setParentId(TreeAbility.ROOT_ID);
        desired.setTitle(item.title());
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(item.sortOrder());
        InitialDataRecord<DictionaryItem> record = InitialDataRecord
                .of(seed.applicationAlias() + "." + seed.alias() + "." + item.code(),
                        InitialDataPolicy.CREATE_IF_MISSING, desired)
                .identity(
                        InitialDataField.of("categoryId", DictionaryItem::getCategoryId,
                                DictionaryItem::setCategoryId),
                        InitialDataField.of("code", DictionaryItem::getCode,
                                DictionaryItem::setCode)
                );
        return InitialDataDeclaration.of(
                record,
                () -> dictionaryItem(categoryId, item.code()),
                itemService::insert,
                itemService::update
        );
    }

    private String itemId(DictionarySeed seed, DictionaryItemSeed item) {
        return item.id() == null ? seed.id() + "." + item.code() : item.id();
    }

    private DictionaryCategory dictionaryCategory(String applicationAlias, String alias) {
        return categoryService.getDao().query(Criteria.of()
                        .eq("applicationAlias", applicationAlias)
                        .eq("alias", alias),
                ONE).stream().findFirst().orElse(null);
    }

    private DictionaryItem dictionaryItem(String categoryId, String code) {
        return itemService.getDao().query(Criteria.of()
                        .eq("categoryId", categoryId)
                        .eq("code", code),
                ONE).stream().findFirst().orElse(null);
    }
}
