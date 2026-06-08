package net.ximatai.muyun.spring.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformNameRulesTest {
    @Test
    void shouldValidateApplicationAliasAndModuleAlias() {
        assertThat(PlatformNameRules.requireApplicationAlias("crm_app")).isEqualTo("crm_app");
        assertThat(PlatformNameRules.requireModuleAlias("crm.customer_profile")).isEqualTo("crm.customer_profile");
        assertThat(PlatformNameRules.requireModuleAlias("platform.code_rule")).isEqualTo("platform.code_rule");

        assertThatThrownBy(() -> PlatformNameRules.requireApplicationAlias("crm-app"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationAlias");
        assertThatThrownBy(() -> PlatformNameRules.requireModuleAlias("crm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
        assertThatThrownBy(() -> PlatformNameRules.requireModuleAlias("platform.codeRule"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias");
    }

    @Test
    void shouldValidateIdentifierCodeAndDatabaseName() {
        assertThat(PlatformNameRules.requireIdentifier("customer_status", "metadataAlias")).isEqualTo("customer_status");
        assertThat(PlatformNameRules.requireCode("active", "dictionaryItemCode")).isEqualTo("active");
        assertThat(PlatformNameRules.requireDatabaseName("crm_customer", "tableName")).isEqualTo("crm_customer");

        assertThatThrownBy(() -> PlatformNameRules.requireIdentifier("Customer", "metadataAlias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadataAlias");
        assertThatThrownBy(() -> PlatformNameRules.requireIdentifier(null, "metadataAlias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadataAlias");
        assertThatThrownBy(() -> PlatformNameRules.requireDatabaseName("crm.customer", "tableName"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableName");
    }

    @Test
    void shouldValidateJavaStyleFieldNameSeparately() {
        assertThat(PlatformNameRules.requireFieldName("customerId", "fieldName")).isEqualTo("customerId");

        assertThatThrownBy(() -> PlatformNameRules.requireFieldName("customer_id", "fieldName"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldName");
    }
}
