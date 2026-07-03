package net.ximatai.muyun.spring.boot.dynamic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicWebRequestTest {
    @AfterEach
    void tearDown() {
        DynamicWebRequest.clearRequestPath();
    }

    @Test
    void shouldResolveModuleAliasFromRequestPathFirstSegment() {
        DynamicWebRequest.useRequestPath("/crm.customer/query");

        assertThat(DynamicWebRequest.moduleAlias()).isEqualTo("crm.customer");
    }

    @Test
    void shouldRequireRequestPathBeforeResolvingModuleAlias() {
        DynamicWebRequest.clearRequestPath();

        assertThatThrownBy(DynamicWebRequest::moduleAlias)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
    }
}
