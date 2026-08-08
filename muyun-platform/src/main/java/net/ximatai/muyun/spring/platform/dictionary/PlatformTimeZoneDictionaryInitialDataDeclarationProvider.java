package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;

import java.util.List;

/**
 * Provides the platform-owned baseline of time zones shared by all applications.
 *
 * <p>Codes are IANA Zone IDs rather than fixed UTC offsets so daylight-saving rules are preserved.
 * Administrators can add further zones through the standard dictionary administration module.</p>
 */
public class PlatformTimeZoneDictionaryInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    public static final String APPLICATION_ALIAS = "platform";
    public static final String CATEGORY_ALIAS = "time_zone";

    private final DictionaryInitialDataDeclarations dictionaries;

    public PlatformTimeZoneDictionaryInitialDataDeclarationProvider(DictionaryInitialDataDeclarations dictionaries) {
        this.dictionaries = dictionaries;
    }

    @Override
    public String name() {
        return "platform.time-zone-dictionary";
    }

    @Override
    public int order() {
        return 18;
    }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        return dictionaries.declare(DictionarySeed.dictionaryFor(APPLICATION_ALIAS, CATEGORY_ALIAS, "时区", 10,
                DictionaryItemSeed.item("Etc/UTC", "协调世界时（UTC）", 10),
                DictionaryItemSeed.item("Asia/Shanghai", "中国标准时间（UTC+8）", 20),
                DictionaryItemSeed.item("Asia/Singapore", "新加坡时间（UTC+8）", 30),
                DictionaryItemSeed.item("Asia/Tokyo", "日本标准时间（UTC+9）", 40),
                DictionaryItemSeed.item("Australia/Perth", "澳大利亚西部时间（UTC+8）", 50),
                DictionaryItemSeed.item("Australia/Brisbane", "澳大利亚东部标准时间（UTC+10，无夏令时）", 60),
                DictionaryItemSeed.item("Europe/Amsterdam", "荷兰时间（CET/CEST，UTC+1 / UTC+2）", 70),
                DictionaryItemSeed.item("Europe/London", "英国时间（GMT/BST）", 80),
                DictionaryItemSeed.item("America/New_York", "美国东部时间（EST/EDT）", 90),
                DictionaryItemSeed.item("America/Los_Angeles", "美国太平洋时间（PST/PDT）", 100),
                DictionaryItemSeed.item("Asia/Dubai", "阿联酋时间（UTC+4）", 110)
        ));
    }
}
