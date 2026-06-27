package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class MetadataService extends AbstractAbilityService<Metadata> implements
        SoftDeleteAbility<Metadata>,
        EnableAbility<Metadata>,
        SortAbility<Metadata>,
        QueryAbility<Metadata> {
    public static final String MODULE_ALIAS = "platform.metadata";
    public static final String DEFAULT_SCHEMA = "public";

    private final ObjectProvider<PlatformMetadataSchemaEnsureService> schemaEnsureServiceProvider;
    private final Optional<PlatformDynamicRuntimeRefreshCoordinator> runtimeRefreshCoordinator;

    public MetadataService(BaseDao<Metadata, String> metadataDao) {
        this(metadataDao, provider(null), Optional.empty());
    }

    public MetadataService(BaseDao<Metadata, String> metadataDao,
                           Optional<PlatformMetadataSchemaEnsureService> schemaEnsureService) {
        this(metadataDao, provider(schemaEnsureService == null ? null : schemaEnsureService.orElse(null)),
                Optional.empty());
    }

    public MetadataService(BaseDao<Metadata, String> metadataDao,
                           Optional<PlatformMetadataSchemaEnsureService> schemaEnsureService,
                           Optional<PlatformDynamicRuntimeRefreshCoordinator> runtimeRefreshCoordinator) {
        this(metadataDao,
                provider(schemaEnsureService == null ? null : schemaEnsureService.orElse(null)),
                runtimeRefreshCoordinator == null ? Optional.empty() : runtimeRefreshCoordinator);
    }

    @Autowired
    public MetadataService(BaseDao<Metadata, String> metadataDao,
                           ObjectProvider<PlatformMetadataSchemaEnsureService> schemaEnsureServiceProvider,
                           Optional<PlatformDynamicRuntimeRefreshCoordinator> runtimeRefreshCoordinator) {
        super(MODULE_ALIAS, Metadata.class, metadataDao);
        this.schemaEnsureServiceProvider = Objects.requireNonNull(schemaEnsureServiceProvider,
                "schemaEnsureServiceProvider must not be null");
        this.runtimeRefreshCoordinator = Objects.requireNonNull(runtimeRefreshCoordinator,
                "runtimeRefreshCoordinator must not be null");
    }

    @Override
    public void beforeInsert(Metadata metadata) {
        normalizeAndValidate(metadata);
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("applicationAlias", QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("所属应用"))
                .field(QueryField.of("alias", QueryOperator.EQ, QueryOperator.IN).withTitle("元数据标识"))
                .field(QueryField.of("schemaName", QueryOperator.EQ).withTitle("数据库Schema"))
                .field(QueryField.of("tableName", QueryOperator.EQ).withTitle("数据库表名"))
                .field(QueryField.of("dataScopeEnabled", QueryValueType.BOOLEAN, QueryOperator.EQ)
                        .withTitle("数据权限"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("名称").withQuickSearch().withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
    }

    @Override
    public void beforeUpdate(Metadata metadata) {
        normalizeAndValidate(metadata);
    }

    @Override
    public Criteria sortScope(Metadata metadata) {
        return sortScopeByFields(metadata, "applicationAlias");
    }

    @Override
    public void validateSortScope(Metadata left, Metadata right) {
        validateSortScopeByFields(left, right,
                "Metadata sort can only move records within the same application", "applicationAlias");
    }

    @Override
    public void afterInsert(String id, Metadata metadata) {
        PlatformMetadataSchemaEnsureService schemaEnsureService = schemaEnsureService();
        if (schemaEnsureService != null) {
            schemaEnsureService.ensure(id);
        }
    }

    @Override
    public void afterUpdate(Metadata metadata, int updated) {
        PlatformMetadataSchemaEnsureService schemaEnsureService = schemaEnsureService();
        if (updated > 0 && schemaEnsureService != null) {
            schemaEnsureService.ensure(metadata.getId());
        }
    }

    @Override
    public void afterChanged(Metadata metadata) {
        PlatformDynamicRuntimeRefreshCoordinator runtimeRefreshCoordinator = runtimeRefreshCoordinator();
        if (runtimeRefreshCoordinator != null) {
            runtimeRefreshCoordinator.refreshByMetadataId(metadata.getId());
        }
    }

    private PlatformMetadataSchemaEnsureService schemaEnsureService() {
        return schemaEnsureServiceProvider.getIfAvailable();
    }

    private PlatformDynamicRuntimeRefreshCoordinator runtimeRefreshCoordinator() {
        return runtimeRefreshCoordinator.orElse(null);
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

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }
}
