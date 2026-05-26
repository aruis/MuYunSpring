package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.BaseModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class InMemoryBaseDao<T extends BaseModel> implements BaseDao<T, String> {
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
        int from = Math.min(pageRequest.getOffset(), filtered.size());
        int to = Math.min(from + pageRequest.getLimit(), filtered.size());
        return new ArrayList<>(filtered.subList(from, to));
    }

    @Override
    public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        List<T> filtered = rows.values().stream()
                .filter(row -> matches(row, criteria))
                .toList();
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
        for (CriteriaClause clause : criteria.getClauses()) {
            Object actual = property(row, clause.getField());
            if (clause.getOperator() == CriteriaOperator.EQ && !clause.getValues().getFirst().equals(actual)) {
                return false;
            }
            if (clause.getOperator() == CriteriaOperator.IN && !clause.getValues().contains(actual)) {
                return false;
            }
        }
        return true;
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
