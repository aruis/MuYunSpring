package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
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
            throw new net.ximatai.muyun.spring.ability.AbilityException("Metadata sort can only move records within the same application");
        }
    }

    private void normalizeAndValidate(Metadata metadata) {
        String applicationAlias = PlatformAliasRules.requireApplicationAlias(metadata.getApplicationAlias());
        String alias = requireIdentifier(metadata.getAlias(), "metadataAlias");
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(alias);
        if (metadata.getSchemaName() == null || metadata.getSchemaName().isBlank()) {
            metadata.setSchemaName(DEFAULT_SCHEMA);
        }
        requireIdentifier(metadata.getSchemaName(), "schemaName");
        if (metadata.getTableName() == null || metadata.getTableName().isBlank()) {
            metadata.setTableName(applicationAlias + "_" + alias);
        }
        requireIdentifier(metadata.getTableName(), "tableName");
        rejectDuplicateMetadataAlias(metadata);
        rejectDuplicatePhysicalTable(metadata);
    }

    private void rejectDuplicateMetadataAlias(Metadata metadata) {
        boolean duplicate = list(Criteria.of()
                .eq("applicationAlias", metadata.getApplicationAlias())
                .eq("alias", metadata.getAlias()), PageRequest.of(1, Integer.MAX_VALUE))
                .stream()
                .anyMatch(existing -> !java.util.Objects.equals(existing.getId(), metadata.getId()));
        if (duplicate) {
            throw new AbilityException("metadataAlias must be unique within application: " + metadata.getAlias());
        }
    }

    private void rejectDuplicatePhysicalTable(Metadata metadata) {
        boolean duplicate = list(Criteria.of()
                .eq("schemaName", metadata.getSchemaName())
                .eq("tableName", metadata.getTableName()), PageRequest.of(1, Integer.MAX_VALUE))
                .stream()
                .anyMatch(existing -> !java.util.Objects.equals(existing.getId(), metadata.getId()));
        if (duplicate) {
            throw new AbilityException("metadata physical table must be unique: "
                    + metadata.getSchemaName() + "." + metadata.getTableName());
        }
    }

    static String requireIdentifier(String value, String name) {
        if (!PlatformAliasRules.isIdentifier(value)) {
            throw new IllegalArgumentException("invalid " + name + ": " + value);
        }
        return value;
    }

    static String requireFieldName(String value, String name) {
        if (value == null || !value.matches("[a-z][A-Za-z0-9]{0,62}")) {
            throw new IllegalArgumentException("invalid " + name + ": " + value);
        }
        return value;
    }
}
