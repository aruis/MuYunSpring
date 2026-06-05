package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;

@Getter
@Setter
@Table(name = "platform_metadata_field_protection_config", comment = "Metadata field protection config")
@CompositeIndex(columns = {"metadata_field_id"}, unique = true)
public class MetadataFieldProtectionConfig extends StandardEntity {
    @Column(name = "metadata_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Metadata field id")
    private String metadataFieldId;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Protection enabled",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "encryption_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Field encryption mode")
    private FieldEncryptionMode encryptionMode = FieldEncryptionMode.NONE;

    @Column(name = "signature_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Field signature mode")
    private FieldSignatureMode signatureMode = FieldSignatureMode.NONE;

    @Column(name = "masking_policy", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Field masking policy")
    private FieldMaskingPolicy maskingPolicy = FieldMaskingPolicy.NONE;

    public FieldProtectionDefinition definition() {
        if (!Boolean.TRUE.equals(enabled)) {
            return FieldProtectionDefinition.NONE;
        }
        return new FieldProtectionDefinition(encryptionMode, signatureMode, maskingPolicy);
    }
}
