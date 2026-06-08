package net.ximatai.muyun.spring.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
class PlatformAliasRulesTest {
    @Test
    void shouldValidateApplicationAlias() {
        assertThat(PlatformAliasRules.requireApplicationAlias("crm_app")).isEqualTo("crm_app");

        assertThatThrownBy(() -> PlatformAliasRules.requireApplicationAlias("crm-app"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationAlias");
    }

    @Test
    void shouldValidateModuleAliasWithDynamicRuntimeCompatibleRule() {
        assertThat(PlatformAliasRules.requireModuleAlias("crm.customer_profile"))
                .isEqualTo("crm.customer_profile");
        assertThat(PlatformAliasRules.requireModuleAlias("platform.code_rule"))
                .isEqualTo("platform.code_rule");

        assertThatThrownBy(() -> PlatformAliasRules.requireModuleAlias("crm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
        assertThatThrownBy(() -> PlatformAliasRules.requireModuleAlias("crm.customer-profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
        assertThatThrownBy(() -> PlatformAliasRules.requireModuleAlias("platform.codeRule"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
    }
}
