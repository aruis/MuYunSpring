package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.common.option.DictionaryFieldDefinition;
import net.ximatai.muyun.spring.common.option.DictionaryFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles dictionary baselines declared by static business fields into platform initial data.
 *
 * <p>Business models only declare their dictionary dependency; this provider remains the sole
 * startup adapter and never gives models direct access to dictionary persistence services.</p>
 */
public class DictionaryFieldInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private final DictionaryInitialDataDeclarations dictionaries;
    private final List<Class<?>> modelClasses;

    public DictionaryFieldInitialDataDeclarationProvider(DictionaryInitialDataDeclarations dictionaries,
                                                          List<Class<?>> modelClasses) {
        this.dictionaries = dictionaries;
        this.modelClasses = modelClasses == null ? List.of() : modelClasses.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(java.util.Comparator.comparing(Class::getName))
                .toList();
    }

    @Override
    public String name() {
        return "platform.static-dictionary-fields";
    }

    @Override
    public int order() {
        return 19;
    }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        return dictionaries.declare(baselines());
    }

    private List<DictionarySeed> baselines() {
        Map<String, DictionaryFieldDefinition> bySource = new LinkedHashMap<>();
        for (Class<?> modelClass : modelClasses) {
            for (DictionaryFieldDefinition definition : DictionaryFieldResolver.resolve(modelClass)) {
                if (!definition.declaresBaseline()) {
                    continue;
                }
                String source = definition.binding().source();
                DictionaryFieldDefinition existing = bySource.putIfAbsent(source, definition);
                if (existing != null) {
                    throw new IllegalArgumentException("multiple dictionary baselines declared for source: " + source);
                }
            }
        }
        return bySource.values().stream().map(this::seed).toList();
    }

    private DictionarySeed seed(DictionaryFieldDefinition definition) {
        OptionBinding.DictionarySource source = definition.binding().dictionarySource();
        DictionaryItemSeed[] items = definition.initialItems().stream()
                .map(item -> DictionaryItemSeed.item(item.code(), item.title(), item.sortOrder()))
                .toArray(DictionaryItemSeed[]::new);
        return DictionarySeed.dictionaryFor(
                source.applicationAlias(),
                source.categoryAlias(),
                definition.title(),
                definition.sortOrder(),
                items
        );
    }

}
