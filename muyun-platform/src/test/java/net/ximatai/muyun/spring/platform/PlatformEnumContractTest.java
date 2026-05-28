package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformEnumContractTest {
    @Test
    void shouldExposeStableCodeAndTitleForPlatformEnums() {
        assertEnumContract(ModuleKind.class);
        assertEnumContract(RelationRole.class);
    }

    private <E extends Enum<E> & CodeTitleEnum> void assertEnumContract(Class<E> enumType) {
        assertThat(Arrays.stream(enumType.getEnumConstants()).map(CodeTitleEnum::getCode))
                .allSatisfy(code -> assertThat(code).isNotBlank())
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(enumType.getEnumConstants()).map(CodeTitleEnum::getTitle))
                .allSatisfy(title -> assertThat(title).isNotBlank());
    }
}
