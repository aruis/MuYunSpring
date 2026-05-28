package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.capability.TreeCapable;

final class DynamicTreeRecord extends DynamicRecordView implements TreeCapable {
    DynamicTreeRecord(DynamicRecord record) {
        super(record);
    }

    @Override
    public String getParentId() {
        return record().parentId();
    }

    @Override
    public void setParentId(String parentId) {
        record().parentId(parentId);
    }

    @Override
    public Integer getSortOrder() {
        return record().sortOrder();
    }

    @Override
    public void setSortOrder(Integer sortOrder) {
        record().sortOrder(sortOrder);
    }
}
