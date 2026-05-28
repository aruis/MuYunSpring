package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class DynamicRecordViewDao<T extends DynamicRecordView> implements BaseDao<T, String> {
    private final DynamicRecordDao delegate;
    private final Function<DynamicRecord, T> wrapper;

    DynamicRecordViewDao(DynamicRecordDao delegate, Function<DynamicRecord, T> wrapper) {
        this.delegate = delegate;
        this.wrapper = wrapper;
    }

    @Override
    public boolean ensureTable() {
        return delegate.ensureTable();
    }

    @Override
    public String insert(T entity) {
        return delegate.insert(entity.record());
    }

    @Override
    public int updateById(T entity) {
        return delegate.updateById(entity.record());
    }

    @Override
    public int updateByIdAndCondition(T entity, Map<String, Object> conditions) {
        return delegate.updateByIdAndCondition(entity.record(), conditions);
    }

    @Override
    public int deleteById(String id) {
        return delegate.deleteById(id);
    }

    @Override
    public int deleteByIdAndCondition(String id, Map<String, Object> conditions) {
        return delegate.deleteByIdAndCondition(id, conditions);
    }

    @Override
    public boolean existsById(String id) {
        return delegate.existsById(id);
    }

    @Override
    public T findById(String id) {
        return wrap(delegate.findById(id));
    }

    @Override
    public List<T> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return delegate.query(criteria, pageRequest, sorts).stream().map(wrapper).toList();
    }

    @Override
    public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        PageResult<DynamicRecord> page = delegate.pageQuery(criteria, pageRequest, sorts);
        return PageResult.of(page.getRecords().stream().map(wrapper).toList(), page.getTotal(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return delegate.count(criteria);
    }

    @Override
    public int upsert(T entity) {
        return delegate.upsert(entity.record());
    }

    private T wrap(DynamicRecord record) {
        return record == null ? null : wrapper.apply(record);
    }
}
