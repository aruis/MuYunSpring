package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import org.springframework.stereotype.Service;

@Service
public class RuntimeAuditEventListener implements RuntimeEventListener {
    private final RuntimeAuditRecordService auditRecordService;

    public RuntimeAuditEventListener(RuntimeAuditRecordService auditRecordService) {
        this.auditRecordService = auditRecordService;
    }

    @Override
    public void onRuntimeEvent(RuntimeEvent event) {
        auditRecordService.record(event);
    }
}
