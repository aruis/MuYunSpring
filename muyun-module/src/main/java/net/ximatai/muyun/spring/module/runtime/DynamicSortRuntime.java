package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SortAbility;

final class DynamicSortRuntime implements SortAbility<DynamicSortRecord> {
    private final DynamicEntityService owner;
    private final BaseDao<DynamicSortRecord, String> dao;

    DynamicSortRuntime(DynamicEntityService owner) {
        this.owner = owner;
        this.dao = new DynamicRecordViewDao<>(owner.dynamicDao(), DynamicSortRecord::new);
    }

    @Override
    public BaseDao<DynamicSortRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return owner.getModuleAlias();
    }

    @Override
    public DynamicSortRecord select(String id) {
        return wrap(owner.select(id));
    }

    @Override
    public DynamicSortRecord selectActiveRaw(String id) {
        return wrap(owner.selectActiveRaw(id));
    }

    @Override
    public int update(DynamicSortRecord entity) {
        return owner.update(entity.record());
    }

    @Override
    public PageResult<DynamicSortRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        PageResult<DynamicRecord> page = owner.pageQuery(criteria, pageRequest, sorts);
        return PageResult.of(page.getRecords().stream().map(DynamicSortRecord::new).toList(), page.getTotal(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return owner.count(criteria);
    }

    @Override
    public Criteria activeCriteria(Criteria criteria) {
        return owner.activeCriteria(criteria);
    }

    private DynamicSortRecord wrap(DynamicRecord record) {
        return record == null ? null : new DynamicSortRecord(record);
    }
}
