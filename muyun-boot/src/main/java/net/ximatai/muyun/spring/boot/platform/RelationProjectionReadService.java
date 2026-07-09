package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class RelationProjectionReadService {
    private final Supplier<RelationProjectionQueryExecutor> projectionQueryExecutor;
    private final RelationProjectionDatabaseTypeProvider databaseTypeProvider;

    public RelationProjectionReadService() {
        this((RelationProjectionQueryExecutor) null, null);
    }

    @Autowired
    public RelationProjectionReadService(ObjectProvider<RelationProjectionQueryExecutor> projectionQueryExecutor,
                                         ObjectProvider<RelationProjectionDatabaseTypeProvider> databaseTypeProvider) {
        this(projectionQueryExecutor == null ? null : projectionQueryExecutor::getIfAvailable,
                databaseTypeProvider == null ? null : databaseTypeProvider.getIfAvailable());
    }

    RelationProjectionReadService(RelationProjectionQueryExecutor projectionQueryExecutor,
                                  RelationProjectionDatabaseTypeProvider databaseTypeProvider) {
        this((Supplier<RelationProjectionQueryExecutor>) () -> projectionQueryExecutor, databaseTypeProvider);
    }

    private RelationProjectionReadService(Supplier<RelationProjectionQueryExecutor> projectionQueryExecutor,
                                          RelationProjectionDatabaseTypeProvider databaseTypeProvider) {
        this.projectionQueryExecutor = projectionQueryExecutor == null ? () -> null : projectionQueryExecutor;
        this.databaseTypeProvider = databaseTypeProvider == null
                ? new RelationProjectionDatabaseTypeProvider()
                : databaseTypeProvider;
    }

    public boolean supportsListQuery(StaticModuleDefinition definition, RecordReadProjection projection) {
        return projectionQueryExecutor() != null
                && definition != null
                && projection != null
                && (!definition.projectionJoins().isEmpty()
                || !definition.references().isEmpty()
                || hasReadProjectionOutput(definition, projection)
                || projection.outputFields().stream()
                        .filter(field -> field.relationCode() != null)
                        .anyMatch(field -> field.relationCode().contains(".")))
                && projection.postReadTransforms().isEmpty()
                && (hasReadProjectionOutput(definition, projection)
                || projection.outputFields().stream().anyMatch(field -> field.relationCode() != null));
    }

    private boolean hasReadProjectionOutput(StaticModuleDefinition definition, RecordReadProjection projection) {
        if (definition.readProjections().isEmpty()) {
            return false;
        }
        java.util.Set<String> outputFields = definition.readProjections().stream()
                .map(StaticModuleReadProjectionDefinition::outputField)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return projection.outputFields().stream()
                .filter(field -> field.relationCode() == null)
                .map(ViewFieldRef::fieldName)
                .anyMatch(outputFields::contains);
    }

    public Optional<PageResult<Map<String, Object>>> queryList(StaticModuleDefinition definition,
                                                              RecordReadProjection projection,
                                                              Criteria criteria,
                                                              PageRequest pageRequest,
                                                              Sort... sorts) {
        return queryList(List.of(definition), definition, projection, criteria, pageRequest, sorts);
    }

    public Optional<PageResult<Map<String, Object>>> queryList(java.util.List<StaticModuleDefinition> definitions,
                                                              StaticModuleDefinition definition,
                                                              RecordReadProjection projection,
                                                              Criteria criteria,
                                                              PageRequest pageRequest,
                                                              Sort... sorts) {
        RelationProjectionQueryExecutor executor = projectionQueryExecutor();
        if (executor == null || !supportsListQuery(definition, projection)) {
            return Optional.empty();
        }
        RelationProjectionSqlPlan plan = RelationProjectionQueryPlanner.plan(
                definitions,
                definition,
                projection,
                databaseTypeProvider.databaseType(),
                requiredMainFields(criteria, sorts)
        );
        if (!plan.hasRelationProjection()) {
            return Optional.empty();
        }
        return Optional.of(executor.page(plan, criteria, pageRequest, sorts));
    }

    private RelationProjectionQueryExecutor projectionQueryExecutor() {
        return projectionQueryExecutor.get();
    }

    private java.util.Set<String> requiredMainFields(Criteria criteria, Sort... sorts) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        if (criteria != null) {
            collectCriteriaFields(criteria.getRoot(), fields);
        }
        if (sorts != null) {
            java.util.Arrays.stream(sorts)
                    .filter(java.util.Objects::nonNull)
                    .map(Sort::getField)
                    .filter(field -> field != null && !field.isBlank())
                    .forEach(fields::add);
        }
        return java.util.Set.copyOf(fields);
    }

    private void collectCriteriaFields(CriteriaGroup group, java.util.Set<String> fields) {
        if (group == null) {
            return;
        }
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            Object node = entry.getNode();
            if (node instanceof CriteriaClause clause) {
                String field = clause.getField();
                if (field != null && !field.isBlank()) {
                    fields.add(field);
                }
            } else if (node instanceof CriteriaGroup childGroup) {
                collectCriteriaFields(childGroup, fields);
            }
        }
    }
}
