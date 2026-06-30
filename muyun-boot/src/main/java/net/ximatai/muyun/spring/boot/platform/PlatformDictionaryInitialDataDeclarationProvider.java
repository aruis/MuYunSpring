package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;

import java.util.List;

import static net.ximatai.muyun.spring.platform.dictionary.DictionaryItemSeed.item;
import static net.ximatai.muyun.spring.platform.dictionary.DictionarySeed.dictionary;

public class PlatformDictionaryInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private final DictionaryInitialDataDeclarations dictionaries;

    public PlatformDictionaryInitialDataDeclarationProvider(DictionaryInitialDataDeclarations dictionaries) {
        this.dictionaries = dictionaries;
    }

    @Override
    public String name() {
        return "platform.required-dictionaries";
    }

    @Override
    public int order() {
        return 19;
    }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        return dictionaries.declare(dictionary(
                "platform.dict.iam.gender",
                "iam",
                "gender",
                "性别",
                10,
                item("1", "男", 10),
                item("2", "女", 20)
        ));
    }
}
