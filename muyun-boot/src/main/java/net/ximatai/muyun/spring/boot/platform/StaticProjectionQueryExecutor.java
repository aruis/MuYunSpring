package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.CompiledCriteria;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SortDirection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnBean(NamedParameterJdbcOperations.class)
public class StaticProjectionQueryExecutor {
    private final NamedParameterJdbcOperations jdbcOperations;
    private final CriteriaSqlCompiler criteriaSqlCompiler = new CriteriaSqlCompiler();
    private final DBInfo.Type databaseType = DBInfo.Type.POSTGRESQL;

    public StaticProjectionQueryExecutor(NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    public PageResult<Map<String, Object>> page(StaticProjectionSqlPlan plan,
                                                Criteria criteria,
                                                PageRequest pageRequest,
                                                Sort... sorts) {
        if (plan == null || !plan.hasRelationProjection()) {
            throw new IllegalArgumentException("projection SQL plan must contain relation projections");
        }
        PageRequest page = pageRequest == null ? PageRequest.of(1, 20) : pageRequest;
        CompiledCriteria compiled = compileCriteria(plan, criteria);
        LinkedHashMap<String, Object> params = new LinkedHashMap<>(plan.baseParams());
        params.putAll(compiled.getParams());
        String where = where(compiled);
        String orderBy = orderBy(plan, sorts);
        String dataSql = "select * from (" + plan.baseSql() + ") q"
                + where
                + orderBy
                + " limit :__limit offset :__offset";
        params.put("__limit", page.getLimit());
        params.put("__offset", page.getOffset());
        List<Map<String, Object>> records = jdbcOperations.queryForList(dataSql, params);

        LinkedHashMap<String, Object> countParams = new LinkedHashMap<>(plan.baseParams());
        countParams.putAll(compiled.getParams());
        Long total = jdbcOperations.queryForObject(
                "select count(*) from (" + plan.baseSql() + ") q" + where,
                countParams,
                Long.class
        );
        return PageResult.of(records, total == null ? 0 : total, page);
    }

    private CompiledCriteria compileCriteria(StaticProjectionSqlPlan plan, Criteria criteria) {
        Criteria actual = criteria == null ? Criteria.of() : criteria;
        return criteriaSqlCompiler.compile(actual, fieldName -> {
            if (!plan.projectedFields().contains(fieldName)) {
                throw new IllegalArgumentException("projection query field is not projected: " + fieldName);
            }
            return StaticProjectionQueryPlanner.quote(fieldName, databaseType);
        }, databaseType);
    }

    private String where(CompiledCriteria criteria) {
        if (criteria == null || criteria.getSql() == null || criteria.getSql().isBlank()) {
            return "";
        }
        return " where " + criteria.getSql();
    }

    private String orderBy(StaticProjectionSqlPlan plan, Sort... sorts) {
        if (sorts == null || sorts.length == 0) {
            return "";
        }
        return " order by " + java.util.Arrays.stream(sorts)
                .map(sort -> {
                    if (!plan.projectedFields().contains(sort.getField())) {
                        throw new IllegalArgumentException("projection sort field is not projected: " + sort.getField());
                    }
                    return StaticProjectionQueryPlanner.quote(sort.getField(), databaseType)
                            + " " + (sort.getDirection() == SortDirection.DESC ? "desc" : "asc");
                })
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
