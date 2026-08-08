package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformTimeZoneDictionaryInitialDataDeclarationProviderTest {
    private final DictionaryCategoryService categoryService = new DictionaryCategoryService(new TestMemoryDao<>());
    private final DictionaryItemService itemService = new DictionaryItemService(new TestMemoryDao<>(), categoryService);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void registersAPlatformOwnedIanaTimeZoneBaseline() {
        new InitialDataExecutor(List.of(), List.of(new PlatformTimeZoneDictionaryInitialDataDeclarationProvider(
                new DictionaryInitialDataDeclarations(categoryService, itemService)))).initializeAll();

        DictionaryCategory category = categoryService.requireDictionaryCategory("platform", "time_zone");
        assertThat(category.getTitle()).isEqualTo("时区");
        assertThat(category.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(itemService.rootItems("platform", "time_zone"))
                .extracting(DictionaryItem::getCode)
                .contains("Asia/Shanghai", "Australia/Perth", "Europe/Amsterdam")
                .doesNotContain("utc_plus_8", "utc_plus_1");
    }
}
