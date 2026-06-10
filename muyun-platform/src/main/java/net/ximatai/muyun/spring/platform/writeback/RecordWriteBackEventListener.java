package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEvent;

public interface RecordWriteBackEventListener {
    void onWriteBackEvent(DynamicRecordMutationEvent event);
}
