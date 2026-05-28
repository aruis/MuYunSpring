package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.model.capability.TitledCapable;

final class DynamicTitledRecord extends DynamicRecordView implements TitledCapable {
    DynamicTitledRecord(DynamicRecord record) {
        super(record);
    }

    @Override
    public String getTitle() {
        return record().title();
    }
}
