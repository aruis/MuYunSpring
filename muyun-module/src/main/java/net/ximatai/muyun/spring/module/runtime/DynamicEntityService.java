package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DynamicEntityService implements CrudAbility<DynamicRecord> {
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

    public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().query(activeCriteria(criteria), pageRequest, sorts);
    }

    public List<DynamicRecord> sortedList(Criteria criteria) {
        requireCapability(EntityCapability.SORT);
        return list(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public void reorder(List<String> orderedIds) {
        Objects.requireNonNull(orderedIds, "orderedIds must not be null");
        requireCapability(EntityCapability.SORT);
        if (orderedIds.isEmpty()) {
            throw new IllegalArgumentException("Cannot reorder empty records");
        }
        Set<String> uniqueIds = new LinkedHashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new IllegalArgumentException("Cannot reorder duplicate records");
        }
        int order = 1;
        for (String id : orderedIds) {
            DynamicRecord record = select(id);
            if (record == null) {
                throw new IllegalArgumentException("Cannot reorder missing record: " + id);
            }
            record.setValue(PlatformAbilityFields.SORT_FIELD, order++);
            update(record);
        }
    }

    public void moveBefore(String id, String beforeId) {
        moveRelative(id, beforeId, true);
    }

    public void moveAfter(String id, String afterId) {
        moveRelative(id, afterId, false);
    }

    public String title(String id) {
        requireCapability(EntityCapability.REFERENCE);
        DynamicRecord record = getDao().findById(id);
        return record == null || Boolean.TRUE.equals(record.getDeleted())
                ? null
                : stringValue(record.getValue(PlatformAbilityFields.TITLE_FIELD));
    }

    public Map<String, String> titles(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        requireCapability(EntityCapability.REFERENCE);
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>(ids);
        List<DynamicRecord> records = getDao().query(
                activeCriteria(Criteria.of().in(StandardEntitySchema.ID_FIELD, List.copyOf(normalizedIds))),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, String> loadedTitles = new LinkedHashMap<>();
        for (DynamicRecord record : records) {
            loadedTitles.put(record.getId(), stringValue(record.getValue(PlatformAbilityFields.TITLE_FIELD)));
        }
        Map<String, String> titles = new LinkedHashMap<>();
        for (String id : normalizedIds) {
            if (loadedTitles.containsKey(id)) {
                titles.put(id, loadedTitles.get(id));
            }
        }
        return titles;
    }

    public PageResult<DynamicReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        requireCapability(EntityCapability.REFERENCE);
        PageResult<DynamicRecord> page = pageQuery(criteria, pageRequest);
        return PageResult.of(
                page.getRecords().stream()
                        .map(record -> new DynamicReferenceOption(record.getId(), stringValue(record.getValue(PlatformAbilityFields.TITLE_FIELD))))
                        .toList(),
                page.getTotal(),
                pageRequest
        );
    }

    private void moveRelative(String id, String targetId, boolean before) {
        DynamicRecord moving = select(id);
        DynamicRecord target = select(targetId);
        if (moving == null || target == null) {
            throw new IllegalArgumentException("Cannot move missing record");
        }
        List<DynamicRecord> rows = sortedList(Criteria.of());
        ArrayList<String> ids = new ArrayList<>();
        for (DynamicRecord row : rows) {
            if (!row.getId().equals(id)) {
                ids.add(row.getId());
            }
        }
        int targetIndex = ids.indexOf(targetId);
        if (targetIndex < 0) {
            throw new IllegalArgumentException("Cannot move before/after missing target: " + targetId);
        }
        ids.add(before ? targetIndex : targetIndex + 1, id);
        reorder(ids);
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
