package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.ReferenceAbility;
import net.ximatai.muyun.spring.ability.ReferenceOption;
import net.ximatai.muyun.spring.ability.ReferenceTarget;

final class DynamicReferenceRuntime implements ReferenceAbility<DynamicTitledRecord> {
    private final DynamicEntityService owner;
    private final BaseDao<DynamicTitledRecord, String> dao;

    DynamicReferenceRuntime(DynamicEntityService owner) {
        this.owner = owner;
        this.dao = new DynamicRecordViewDao<>(owner.dynamicDao(), DynamicTitledRecord::new);
    }

    @Override
    public BaseDao<DynamicTitledRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return owner.getModuleAlias();
    }

    @Override
    public ReferenceTarget referenceTarget() {
        return owner.referenceTarget();
    }

    @Override
    public DynamicTitledRecord select(String id) {
        return wrap(owner.select(id));
    }

    @Override
    public DynamicTitledRecord selectActiveRaw(String id) {
        return wrap(owner.selectActiveRaw(id));
    }

    @Override
    public DynamicTitledRecord selectReferenceRaw(String id) {
        return wrap(owner.activeRaw(id));
    }

    @Override
    public String referenceTitle(DynamicTitledRecord entity) {
        return entity == null ? null : entity.getTitle();
    }

    @Override
    public PageResult<DynamicTitledRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        PageResult<DynamicRecord> page = owner.pageQuery(criteria, pageRequest, sorts);
        return PageResult.of(page.getRecords().stream().map(DynamicTitledRecord::new).toList(), page.getTotal(), pageRequest);
    }

    @Override
    public long count(Criteria criteria) {
        return owner.count(criteria);
    }

    @Override
    public Criteria activeCriteria(Criteria criteria) {
        return owner.activeCriteria(criteria);
    }

    @Override
    public PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        return ReferenceAbility.super.referenceOptions(criteria, pageRequest);
    }

    private DynamicTitledRecord wrap(DynamicRecord record) {
        return record == null ? null : new DynamicTitledRecord(record);
    }
}
