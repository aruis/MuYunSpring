package net.ximatai.muyun.spring.dynamic.runtime.mapping;

import net.ximatai.muyun.database.core.orm.RuntimeColumnMapper;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicRecordMappingTest {
    @Test
    void shouldImplementRuntimeColumnMapperContract() {
        DynamicRecordMapping mapping = new DynamicRecordMapping(contractEntity());
        RuntimeColumnMapper mapper = mapping;

        assertThat(mapper.resolveColumnName("customerName")).isEqualTo("customer_name");
        assertThat(mapper.resolveColumnName("customer_name")).isEqualTo("customer_name");
        assertThat(mapper.resolveFieldName("customer_name")).isEqualTo("customerName");
        assertThat(mapper.resolveColumnName("missing")).isNull();

        assertThatThrownBy(() -> mapping.resolveColumn("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown dynamic field or column");
    }

    @Test
    void shouldKeepPlatformProtectedStorageFieldsOutOfQueries() {
        DynamicRecordMapping mapping = new DynamicRecordMapping(contractEntity());

        assertThatThrownBy(() -> mapping.resolveQueryableColumn("secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protected storage field cannot be used");
        assertThatThrownBy(() -> mapping.resolveQueryableColumn("secret_signature"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protected storage field cannot be used");
    }

    @Test
    void shouldMapDynamicApprovalAbilityFields() {
        DynamicRecordMapping mapping = new DynamicRecordMapping(contractEntity().withCapabilities(EntityCapability.APPROVAL));
        RuntimeColumnMapper mapper = mapping;

        assertThat(mapper.resolveColumnName(PlatformAbilityFields.APPROVAL_STATUS_FIELD))
                .isEqualTo(PlatformAbilityFields.APPROVAL_STATUS_COLUMN);
        assertThat(mapper.resolveFieldName(PlatformAbilityFields.APPROVAL_STATUS_COLUMN))
                .isEqualTo(PlatformAbilityFields.APPROVAL_STATUS_FIELD);
        assertThat(mapping.resolveQueryableColumn(PlatformAbilityFields.APPROVAL_STATUS_FIELD))
                .isEqualTo(PlatformAbilityFields.APPROVAL_STATUS_COLUMN);
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("customerName", "Customer Name").column("customer_name"),
                        FieldDefinition.string("secret", "Secret")
                                .protection(new FieldProtectionDefinition(
                                        FieldEncryptionMode.ENCRYPTED,
                                        FieldSignatureMode.SIGNED,
                                        FieldMaskingPolicy.NONE
                                ))
                )
        );
    }
}
