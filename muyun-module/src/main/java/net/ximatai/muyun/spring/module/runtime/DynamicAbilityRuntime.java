package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

abstract class DynamicAbilityRuntime<T extends DynamicRecordView> implements CrudAbility<T> {
    protected final DynamicEntityService owner;
    private final Function<DynamicRecord, T> wrapper;
    private final BaseDao<T, String> dao;

    DynamicAbilityRuntime(DynamicEntityService owner, Function<DynamicRecord, T> wrapper) {
        this.owner = owner;
        this.wrapper = wrapper;
        this.dao = new DynamicRecordViewDao<>(owner.dynamicDao(), wrapper);
    }

    @Override
    public BaseDao<T, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return owner.getModuleAlias();
    }

    @Override
    public String insert(T entity) {
        throw unsupportedMutation("insert");
    }

    @Override
    public List<String> insertBatch(Collection<T> entities) {
        throw unsupportedMutation("insertBatch");
    }

    @Override
    public T select(String id) {
        return wrap(owner.select(id));
    }

    @Override
    public T selectActiveRaw(String id) {
        return wrap(owner.selectActiveRaw(id));
    }

    @Override
    public int update(T entity) {
        return owner.update(entity.record());
    }

    @Override
    public int delete(String id) {
        throw unsupportedMutation("delete");
    }

    @Override
    public int delete(T entity) {
        throw unsupportedMutation("delete");
    }

    @Override
    public int delete(String id, Integer expectedVersion) {
        throw unsupportedMutation("delete");
    }

    @Override
    public int deleteBatch(Collection<String> ids) {
        throw unsupportedMutation("deleteBatch");
    }

    @Override
    public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        PageResult<DynamicRecord> page = owner.pageQuery(criteria, pageRequest, sorts);
        return PageResult.of(page.getRecords().stream().map(wrapper).toList(), page.getTotal(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return owner.count(criteria);
    }

    @Override
    public Criteria activeCriteria(Criteria criteria) {
        return owner.activeCriteria(criteria);
    }

    protected T wrap(DynamicRecord record) {
        return record == null ? null : wrapper.apply(record);
    }

    private UnsupportedOperationException unsupportedMutation(String operation) {
        return new UnsupportedOperationException(
                "dynamic ability runtime " + operation + " must go through DynamicEntityService"
        );
    }
}
