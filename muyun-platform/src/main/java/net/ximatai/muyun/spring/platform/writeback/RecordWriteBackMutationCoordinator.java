package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEvent;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RecordWriteBackMutationCoordinator implements DynamicRecordMutationCoordinator {
    private final RecordWriteBackRuntimeService runtimeService;

    public RecordWriteBackMutationCoordinator(RecordWriteBackRuntimeService runtimeService) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService must not be null");
    }

    @Override
    public void afterMutation(DynamicRecordMutationEvent event) {
        runtimeService.onMutationEvent(event);
    }
}
