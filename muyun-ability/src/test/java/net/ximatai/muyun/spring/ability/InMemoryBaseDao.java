package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityContract;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class InMemoryBaseDao<T extends EntityContract> implements BaseDao<T, String> {
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
        if (!rows.containsKey(entity.getId())) {
            return 0;
        }
        rows.put(entity.getId(), entity);
        return 1;
    }

    @Override
    public int deleteById(String id) {
        return rows.remove(id) == null ? 0 : 1;
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
                .toList();
        filtered = sort(filtered, sorts);
        int from = Math.min(pageRequest.getOffset(), filtered.size());
        int to = Math.min(from + pageRequest.getLimit(), filtered.size());
        return new ArrayList<>(filtered.subList(from, to));
    }

    @Override
    public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        List<T> filtered = rows.values().stream()
                .filter(row -> matches(row, criteria))
                .toList();
        filtered = sort(filtered, sorts);
        int from = Math.min(pageRequest.getOffset(), filtered.size());
        int to = Math.min(from + pageRequest.getLimit(), filtered.size());
        return PageResult.of(new ArrayList<>(filtered.subList(from, to)), filtered.size(), pageRequest);
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

    private boolean isOrJoin(CriteriaGroup.Entry entry) {
        try {
            Method method = entry.getClass().getMethod("getJoin");
            return "OR".equals(String.valueOf(method.invoke(entry)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read criteria join", e);
        }
    }

    private boolean matchesNode(T row, Object node) {
        if (node instanceof CriteriaClause clause) {
            return matchesClause(row, clause);
        }
        if (node instanceof CriteriaGroup group) {
            return matchesGroup(row, group);
        }
        throw new IllegalArgumentException("Unsupported criteria node: " + node);
    }

    private boolean matchesClause(T row, CriteriaClause clause) {
        Object actual = property(row, clause.getField());
        if (clause.getOperator() == CriteriaOperator.EQ) {
            return equalsNullable(clause.getValues().getFirst(), actual);
        }
        if (clause.getOperator() == CriteriaOperator.IN) {
            return clause.getValues().contains(actual);
        }
        if (clause.getOperator() == CriteriaOperator.IS_NULL) {
            return actual == null;
        }
        if (clause.getOperator() == CriteriaOperator.IS_NOT_NULL) {
            return actual != null;
        }
        throw new UnsupportedOperationException("Unsupported in-memory criteria operator: " + clause.getOperator());
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    @SuppressWarnings("unchecked")
    private List<T> sort(List<T> rows, Sort... sorts) {
        if (sorts == null || sorts.length == 0) {
            return rows;
        }
        List<T> sorted = new ArrayList<>(rows);
        for (int i = sorts.length - 1; i >= 0; i--) {
            Sort sort = sorts[i];
            Comparator<T> comparator = Comparator.comparing(
                    row -> (Comparable<Object>) property(row, sort.getField()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            if (sort.getDirection().name().equals("DESC")) {
                comparator = comparator.reversed();
            }
            sorted.sort(comparator);
        }
        return sorted;
    }

    private Object property(T row, String field) {
        String methodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
        try {
            Method method = row.getClass().getMethod(methodName);
            return method.invoke(row);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("No getter for field: " + field, e);
        }
    }
}
