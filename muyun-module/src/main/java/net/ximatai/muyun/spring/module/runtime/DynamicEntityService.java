package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.ReferenceAbility;
import net.ximatai.muyun.spring.ability.ReferenceOption;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicEntityService implements
        CrudAbility<DynamicRecord>,
        SoftDeleteAbility<DynamicRecord>,
        TreeAbility<DynamicRecord>,
        ReferenceAbility<DynamicRecord> {
    private final DynamicRecordDao dao;
    private final String moduleAlias;
    private final DynamicRecordLifecycle lifecycle;

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias) {
        this(dao, moduleAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias, DynamicRecordLifecycle lifecycle) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.moduleAlias = requireModuleAlias(moduleAlias);
        this.lifecycle = lifecycle == null ? DynamicRecordLifecycle.NONE : lifecycle;
    }

    @Override
    public BaseDao<DynamicRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return moduleAlias;
    }

    @Override
    public void beforeInsert(DynamicRecord record) {
        lifecycle.beforeInsert(record);
        record.validateForInsert();
    }

    @Override
    public void beforeUpdate(DynamicRecord record) {
        lifecycle.beforeUpdate(record);
    }

    @Override
    public void beforeDelete(String id) {
        lifecycle.beforeDelete(id);
    }

    @Override
    public void afterSelect(DynamicRecord record) {
        lifecycle.afterSelect(record);
    }

    @Override
    public Integer nextVersionForUpdate(DynamicRecord record) {
        if (record.getVersion() != null) {
            return EntityLifecycle.nextVersion(record.getVersion());
        }
        DynamicRecord current = activeRaw(record.getId());
        if (current == null) {
            throw new IllegalArgumentException("dynamic record not found: " + record.getId());
        }
        return EntityLifecycle.nextVersion(current.getVersion());
    }

    @Override
    public boolean shouldPrepareTreeDefault(DynamicRecord record) {
        return dao.getEntity().supports(EntityCapability.TREE);
    }

    @Override
    public boolean shouldPrepareEnabledDefault(DynamicRecord record) {
        return dao.getEntity().supports(EntityCapability.TREE);
    }

    public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().query(activeCriteria(criteria), pageRequest, sorts);
    }

    public List<DynamicRecord> sortedList(Criteria criteria) {
        requireCapability(EntityCapability.SORT);
        return TreeAbility.super.sortedList(criteria);
    }

    public void reorder(List<String> orderedIds) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.reorder(orderedIds);
    }

    public void moveBefore(String id, String beforeId) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.moveBefore(id, beforeId);
    }

    public void moveAfter(String id, String afterId) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.moveAfter(id, afterId);
    }

    @Override
    public List<DynamicRecord> children(String parentId) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.children(parentId);
    }

    @Override
    public List<String> ancestorIds(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.ancestorIds(id);
    }

    @Override
    public List<String> ancestorIdsAndSelf(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.ancestorIdsAndSelf(id);
    }

    @Override
    public List<String> descendantIds(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.descendantIds(id);
    }

    @Override
    public void validateTreePlacement(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            TreeAbility.super.validateTreePlacement(record);
        }
    }

    @Override
    public Criteria sortScope(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            return TreeAbility.super.sortScope(record);
        }
        return Criteria.of();
    }

    @Override
    public void validateSortScope(DynamicRecord left, DynamicRecord right) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            TreeAbility.super.validateSortScope(left, right);
        }
    }

    public String title(String id) {
        requireCapability(EntityCapability.REFERENCE);
        return ReferenceAbility.super.title(id);
    }

    public Map<String, String> titles(Collection<String> ids) {
        requireCapability(EntityCapability.REFERENCE);
        return ReferenceAbility.super.titles(ids);
    }

    public PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        requireCapability(EntityCapability.REFERENCE);
        return ReferenceAbility.super.referenceOptions(criteria, pageRequest);
    }

    private DynamicRecord activeRaw(String id) {
        return getDao().query(activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void requireCapability(EntityCapability capability) {
        if (!dao.getEntity().supports(capability)) {
            throw new IllegalStateException("dynamic entity does not support capability: " + capability);
        }
    }

    private String requireModuleAlias(String value) {
        Objects.requireNonNull(value, "moduleAlias must not be null");
        if (!value.contains(".")) {
            throw new IllegalArgumentException("dynamic moduleAlias must be a platform module alias: " + value);
        }
        return value;
    }
}
