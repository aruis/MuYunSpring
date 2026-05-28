package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.TreeAbility;

final class DynamicTreeRuntime implements TreeAbility<DynamicTreeRecord> {
    private final DynamicEntityService owner;
    private final BaseDao<DynamicTreeRecord, String> dao;

    DynamicTreeRuntime(DynamicEntityService owner) {
        this.owner = owner;
        this.dao = new DynamicRecordViewDao<>(owner.dynamicDao(), DynamicTreeRecord::new);
    }

    @Override
    public BaseDao<DynamicTreeRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return owner.getModuleAlias();
    }

    @Override
    public DynamicTreeRecord select(String id) {
        return wrap(owner.select(id));
    }

    @Override
    public DynamicTreeRecord selectActiveRaw(String id) {
        return wrap(owner.selectActiveRaw(id));
    }

    @Override
    public int update(DynamicTreeRecord entity) {
        return owner.update(entity.record());
    }

    @Override
    public PageResult<DynamicTreeRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        PageResult<DynamicRecord> page = owner.pageQuery(criteria, pageRequest, sorts);
        return PageResult.of(page.getRecords().stream().map(DynamicTreeRecord::new).toList(), page.getTotal(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return owner.count(criteria);
    }

    @Override
    public Criteria activeCriteria(Criteria criteria) {
        return owner.activeCriteria(criteria);
    }

    private DynamicTreeRecord wrap(DynamicRecord record) {
        return record == null ? null : new DynamicTreeRecord(record);
    }
}
