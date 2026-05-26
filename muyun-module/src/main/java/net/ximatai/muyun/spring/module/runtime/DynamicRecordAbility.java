package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.BaseModelLifecycle;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DynamicRecordAbility implements CrudAbility<DynamicRecord> {
    private final DynamicRecordDao dao;
    private final String moduleAlias;
    private final DynamicRecordLifecycle lifecycle;

    public DynamicRecordAbility(DynamicRecordDao dao, String moduleAlias) {
        this(dao, moduleAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicRecordAbility(DynamicRecordDao dao, String moduleAlias, DynamicRecordLifecycle lifecycle) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.moduleAlias = Objects.requireNonNull(moduleAlias, "moduleAlias must not be null");
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
            return BaseModelLifecycle.nextVersion(record.getVersion());
        }
        DynamicRecord current = dao.findById(record.getId());
        if (current == null) {
            throw new IllegalArgumentException("dynamic record not found: " + record.getId());
        }
        return BaseModelLifecycle.nextVersion(current.getVersion());
    }

    public String getSortField() {
        return dao.getEntity().fields().stream()
                .filter(FieldDefinition::isSortable)
                .map(FieldDefinition::code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("dynamic entity has no sortable field: " + dao.getEntity().code()));
    }

    public List<DynamicRecord> sortedList(Criteria criteria) {
        return getDao().query(activeCriteria(criteria), new PageRequest(0, Integer.MAX_VALUE), Sort.asc(getSortField()));
    }

    public void reorder(List<String> orderedIds) {
        Objects.requireNonNull(orderedIds, "orderedIds must not be null");
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
            record.setValue(getSortField(), order++);
            update(record);
        }
    }

    public void moveBefore(String id, String beforeId) {
        moveRelative(id, beforeId, true);
    }

    public void moveAfter(String id, String afterId) {
        moveRelative(id, afterId, false);
    }

    public String getTitleField() {
        return dao.getEntity().fields().stream()
                .filter(FieldDefinition::isTitle)
                .map(FieldDefinition::code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("dynamic entity has no title field: " + dao.getEntity().code()));
    }

    public String title(String id) {
        String titleField = getTitleField();
        DynamicRecord record = getDao().findById(id);
        return record == null || Boolean.TRUE.equals(record.getDeleted()) ? null : stringValue(record.getValue(titleField));
    }

    public Map<String, String> titles(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<DynamicRecord> records = getDao().query(
                activeCriteria(Criteria.of().in("id", List.copyOf(ids))),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, String> titles = new LinkedHashMap<>();
        String titleField = getTitleField();
        for (DynamicRecord record : records) {
            titles.put(record.getId(), stringValue(record.getValue(titleField)));
        }
        return titles;
    }

    public PageResult<DynamicReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        PageResult<DynamicRecord> page = pageQuery(criteria, pageRequest);
        String titleField = getTitleField();
        return PageResult.of(
                page.getRecords().stream()
                        .map(record -> new DynamicReferenceOption(record.getId(), stringValue(record.getValue(titleField))))
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
