package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.builder.sql.SchemaBuildRules;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.OrmException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicCriteriaSqlCompiler {
    private final DynamicRecordMapping mapping;
    private final DBInfo.Type databaseType;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private int paramIndex;

    DynamicCriteriaSqlCompiler(DynamicRecordMapping mapping, DBInfo.Type databaseType) {
        this.mapping = mapping;
        this.databaseType = databaseType;
    }

    Compiled compile(Criteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return new Compiled("", Map.of());
        }
        return new Compiled(compileGroup(criteria.getRoot()), Map.copyOf(params));
    }

    private String compileGroup(CriteriaGroup group) {
        List<String> parts = new ArrayList<>();
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            String expression = compileNode(entry.getNode());
            if (expression.isBlank()) {
                continue;
            }
            Object join = entry.getJoin();
            parts.add(parts.isEmpty() ? expression : join + " " + expression);
        }
        return String.join(" ", parts);
    }

    private String compileNode(Object node) {
        if (node instanceof CriteriaClause clause) {
            return compileClause(clause);
        }
        if (node instanceof CriteriaGroup group) {
            String nested = compileGroup(group);
            return nested.isBlank() ? "" : "(" + nested + ")";
        }
        throw invalid("Unsupported criteria node");
    }

    private String compileClause(CriteriaClause clause) {
        CriteriaOperator operator = clause.getOperator();
        if (operator == CriteriaOperator.EQ) {
            return compare(clause, "=");
        }
        if (operator == CriteriaOperator.NE) {
            return compare(clause, "<>");
        }
        if (operator == CriteriaOperator.GT) {
            return compare(clause, ">");
        }
        if (operator == CriteriaOperator.GTE) {
            return compare(clause, ">=");
        }
        if (operator == CriteriaOperator.LT) {
            return compare(clause, "<");
        }
        if (operator == CriteriaOperator.LTE) {
            return compare(clause, "<=");
        }
        if (operator == CriteriaOperator.LIKE) {
            return compare(clause, "LIKE");
        }
        if (operator == CriteriaOperator.IS_NULL) {
            return column(clause) + " IS NULL";
        }
        if (operator == CriteriaOperator.IS_NOT_NULL) {
            return column(clause) + " IS NOT NULL";
        }
        if (operator == CriteriaOperator.BETWEEN) {
            return between(clause);
        }
        if (operator == CriteriaOperator.IN) {
            return in(clause, false);
        }
        if (operator == CriteriaOperator.NOT_IN) {
            return in(clause, true);
        }
        throw invalid("Unsupported criteria operator: " + operator);
    }

    private String compare(CriteriaClause clause, String operator) {
        String key = nextKey("p");
        params.put(key, firstValue(clause));
        return column(clause) + " " + operator + " :" + key;
    }

    private String between(CriteriaClause clause) {
        if (clause.getValues().size() != 2) {
            throw invalid("BETWEEN requires exactly 2 values");
        }
        String start = nextKey("p");
        String end = nextKey("p");
        params.put(start, clause.getValues().get(0));
        params.put(end, clause.getValues().get(1));
        return column(clause) + " BETWEEN :" + start + " AND :" + end;
    }

    private String in(CriteriaClause clause, boolean not) {
        if (clause.getValues().isEmpty()) {
            return not ? "1 = 1" : "1 = 0";
        }
        List<String> holders = new ArrayList<>();
        for (Object value : clause.getValues()) {
            String key = nextKey("p");
            holders.add(":" + key);
            params.put(key, value);
        }
        return column(clause) + (not ? " NOT IN (" : " IN (") + String.join(", ", holders) + ")";
    }

    private String column(CriteriaClause clause) {
        String column = mapping.resolveColumn(clause.getField());
        return SchemaBuildRules.quoteIdentifier(column, databaseType);
    }

    private Object firstValue(CriteriaClause clause) {
        if (clause.getValues().isEmpty()) {
            throw invalid(clause.getOperator() + " requires one value");
        }
        return clause.getValues().getFirst();
    }

    private String nextKey(String prefix) {
        return prefix + paramIndex++;
    }

    private OrmException invalid(String message) {
        return new OrmException(OrmException.Code.INVALID_CRITERIA, message);
    }

    record Compiled(String sql, Map<String, Object> params) {
    }
}
