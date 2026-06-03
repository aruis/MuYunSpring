package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class MetadataService extends AbstractAbilityService<Metadata> implements
        SoftDeleteAbility<Metadata>,
        EnableAbility<Metadata>,
        SortAbility<Metadata> {
    public static final String MODULE_ALIAS = "platform.metadata";
    public static final String DEFAULT_SCHEMA = "public";

    public MetadataService(BaseDao<Metadata, String> metadataDao) {
        super(MODULE_ALIAS, Metadata.class, metadataDao);
    }

    @Override
    public void beforeInsert(Metadata metadata) {
        normalizeAndValidate(metadata);
    }

    @Override
    public void beforeUpdate(Metadata metadata) {
        normalizeAndValidate(metadata);
    }

    @Override
    public Criteria sortScope(Metadata metadata) {
        return Criteria.of().eq("applicationAlias", metadata.getApplicationAlias());
    }

    @Override
    public void validateSortScope(Metadata left, Metadata right) {
        if (!java.util.Objects.equals(left.getApplicationAlias(), right.getApplicationAlias())) {
            throw new PlatformException("Metadata sort can only move records within the same application");
        }
    }

    private void normalizeAndValidate(Metadata metadata) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(metadata.getApplicationAlias());
        String alias = PlatformNameRules.requireIdentifier(metadata.getAlias(), "metadataAlias");
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(alias);
        if (metadata.getSchemaName() == null || metadata.getSchemaName().isBlank()) {
            metadata.setSchemaName(DEFAULT_SCHEMA);
        }
        PlatformNameRules.requireDatabaseName(metadata.getSchemaName(), "schemaName");
        if (metadata.getTableName() == null || metadata.getTableName().isBlank()) {
            metadata.setTableName(applicationAlias + "_" + alias);
        }
        PlatformNameRules.requireDatabaseName(metadata.getTableName(), "tableName");
        if (metadata.getDataScopeEnabled() == null) {
            metadata.setDataScopeEnabled(Boolean.FALSE);
        }
        rejectDuplicateMetadataAlias(metadata);
        rejectDuplicatePhysicalTable(metadata);
    }

    private void rejectDuplicateMetadataAlias(Metadata metadata) {
        rejectDuplicate(metadata, Criteria.of()
                .eq("applicationAlias", metadata.getApplicationAlias())
                .eq("alias", metadata.getAlias()),
                "metadataAlias must be unique within application: " + metadata.getAlias());
    }

    private void rejectDuplicatePhysicalTable(Metadata metadata) {
        rejectDuplicate(metadata, Criteria.of()
                .eq("schemaName", metadata.getSchemaName())
                .eq("tableName", metadata.getTableName()),
                "metadata physical table must be unique: " + metadata.getSchemaName() + "." + metadata.getTableName());
    }
}
