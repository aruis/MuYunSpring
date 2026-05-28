package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.TitledCapable;

final class DynamicTitledRecord extends DynamicRecordView implements TitledCapable {
    DynamicTitledRecord(DynamicRecord record) {
        super(record);
    }

    @Override
    public String getTitle() {
        return record().title();
    }
}
