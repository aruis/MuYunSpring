package net.ximatai.muyun.spring.platform.support;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SortDirection;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestMemoryDao<T extends EntityContract> implements BaseDao<T, String> {
    private final Map<String, T> rows = new LinkedHashMap<>();

    @Override
    public boolean ensureTable() {
        return true;
    }

    @Override
    public String insert(T entity) {
        rows.put(entity.getId(), entity);
        return entity.getId();
    }

    @Override
    public int updateById(T entity) {
        rows.put(entity.getId(), entity);
        return 1;
    }

    @Override
    public int updateByIdAndCondition(T entity, Map<String, Object> conditions) {
        rows.put(entity.getId(), entity);
        return 1;
    }

    @Override
    public int deleteById(String id) {
        return rows.remove(id) == null ? 0 : 1;
    }

    @Override
    public int deleteByIdAndCondition(String id, Map<String, Object> conditions) {
        return deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return rows.containsKey(id);
    }

    @Override
    public T findById(String id) {
        return rows.get(id);
    }

    @Override
    public List<T> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        List<T> filtered = rows.values().stream()
                .filter(row -> matches(row, criteria))
                .sorted(comparator(sorts))
                .toList();
        int from = Math.min(pageRequest.getOffset(), filtered.size());
        int to = Math.min(from + pageRequest.getLimit(), filtered.size());
        return new ArrayList<>(filtered.subList(from, to));
    }

    @Override
    public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        List<T> records = query(criteria, pageRequest, sorts);
        return PageResult.of(records, records.size(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return rows.values().stream().filter(row -> matches(row, criteria)).count();
    }

    @Override
    public int upsert(T entity) {
        rows.put(entity.getId(), entity);
        return 1;
    }

    private Integer sortOrder(T row) {
        return row instanceof SortCapable sortable ? sortable.getSortOrder() : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparator<T> comparator(Sort... sorts) {
        if (sorts == null || sorts.length == 0) {
            return Comparator.comparing(this::sortOrder, Comparator.nullsLast(Integer::compareTo));
        }
        Comparator<T> comparator = null;
        for (Sort sort : sorts) {
            if (sort == null) {
                continue;
            }
            Comparator<Comparable> valueComparator = sort.getDirection() == SortDirection.DESC
                    ? Comparator.nullsLast(Comparator.reverseOrder())
                    : Comparator.nullsLast(Comparator.naturalOrder());
            Comparator<T> next = Comparator.comparing(row -> comparableValue(row, sort.getField()), valueComparator);
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? Comparator.comparing(this::sortOrder, Comparator.nullsLast(Integer::compareTo)) : comparator;
    }

    @SuppressWarnings("rawtypes")
    private Comparable comparableValue(T row, String field) {
        Object value = value(row, field);
        return value instanceof Comparable comparable ? comparable : null;
    }

    private boolean matches(T row, Criteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        return matchesGroup(row, criteria.getRoot());
    }

    private boolean matchesGroup(T row, CriteriaGroup group) {
        Boolean matched = null;
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            boolean entryMatched = matchesNode(row, entry.getNode());
            if (matched == null) {
                matched = entryMatched;
            } else if (isOrJoin(entry)) {
                matched = matched || entryMatched;
            } else {
                matched = matched && entryMatched;
            }
        }
        return matched == null || matched;
    }

    private boolean matchesNode(T row, Object node) {
        if (node instanceof CriteriaClause clause) {
            return matchesClause(row, clause);
        }
        if (node instanceof CriteriaGroup group) {
            return matchesGroup(row, group);
        }
        return true;
    }

    private boolean matchesClause(T row, CriteriaClause clause) {
        Object actual = value(row, clause.getField());
        if (clause.getOperator() == CriteriaOperator.IS_NULL) {
            return actual == null;
        }
        if (clause.getOperator() == CriteriaOperator.IS_NOT_NULL) {
            return actual != null;
        }
        if (clause.getOperator() == CriteriaOperator.EQ) {
            Object expected = clause.getValues().getFirst();
            return expected == null ? actual == null : expected.equals(actual);
        }
        if (clause.getOperator() == CriteriaOperator.IN) {
            return clause.getValues().stream()
                    .flatMap(value -> value instanceof Collection<?> collection ? collection.stream() : java.util.stream.Stream.of(value))
                    .anyMatch(expected -> expected == null ? actual == null : expected.equals(actual));
        }
        return true;
    }

    private Object value(T row, String field) {
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            return row.getClass().getMethod(getter).invoke(row);
        } catch (ReflectiveOperationException e) {
            try {
                String getter = "is" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                return row.getClass().getMethod(getter).invoke(row);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }

    private boolean isOrJoin(CriteriaGroup.Entry entry) {
        try {
            Method method = entry.getClass().getMethod("getJoin");
            return "OR".equals(String.valueOf(method.invoke(entry)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read criteria join", e);
        }
    }
}
