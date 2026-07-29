package net.ximatai.muyun.spring.boot.demo.school.configuration;

import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;

import java.util.List;

import static net.ximatai.muyun.spring.platform.dictionary.DictionaryItemSeed.item;
import static net.ximatai.muyun.spring.platform.dictionary.DictionarySeed.dictionary;

/** 通过平台标准初始数据机制声明教学学科字典，而非由教师业务自行维护枚举。 */
public class TeachingDictionaryInitialDataProvider implements InitialDataDeclarationProvider {
    private final DictionaryInitialDataDeclarations dictionaries;

    public TeachingDictionaryInitialDataProvider(DictionaryInitialDataDeclarations dictionaries) {
        this.dictionaries = dictionaries;
    }

    @Override
    public String name() {
        return "education.teaching-subject-dictionary";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        return dictionaries.declare(dictionary(
                "edu.dict.subject",
                "education",
                "teaching_subject",
                "教学学科",
                100,
                item("mathematics", "数学", 10),
                item("chinese", "语文", 20),
                item("english", "英语", 30)
        ));
    }
}
