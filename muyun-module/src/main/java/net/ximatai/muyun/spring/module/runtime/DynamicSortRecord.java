package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.SortCapable;

final class DynamicSortRecord extends DynamicRecordView implements SortCapable {
    DynamicSortRecord(DynamicRecord record) {
        super(record);
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
