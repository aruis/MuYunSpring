package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class RuntimeAuditRecordService extends AbstractAbilityService<RuntimeAuditRecord> {
    public static final String MODULE_ALIAS = "platform.runtime_audit_record";

    public RuntimeAuditRecordService(BaseDao<RuntimeAuditRecord, String> auditRecordDao) {
        super(MODULE_ALIAS, RuntimeAuditRecord.class, auditRecordDao);
    }

    public String record(RuntimeEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        RuntimeAuditRecord record = new RuntimeAuditRecord();
        record.setEventId(event.eventId());
        record.setTenantId(event.tenantId());
        record.setTraceId(event.traceId());
        record.setEventType(event.eventType());
        record.setModuleAlias(event.moduleAlias());
        record.setEntityAlias(event.entityAlias());
        record.setRecordId(event.recordId());
        record.setActionCode(event.actionCode());
        fillActionPayload(record, event);
        record.setSystemContext(event.systemContext());
        record.setMutationSource(event.mutationSource());
        record.setPayloadText(payloadText(event.payload()));
        record.setOccurredAt(event.occurredAt());
        if (event.tenantId() == null) {
            try (TenantContext.Scope ignored = TenantContext.system()) {
                return insert(record);
            }
        }
        return insert(record);
    }

    private void fillActionPayload(RuntimeAuditRecord record, RuntimeEvent event) {
        if (event.eventType() != RuntimeEventType.ACTION_EXECUTED) {
            return;
        }
        record.setExecutorType(textPayload(event.payload(), "executorType"));
        record.setResultType(textPayload(event.payload(), "resultType"));
        record.setResultMessage(textPayload(event.payload(), "message"));
        record.setRefreshRequested(booleanPayload(event.payload(), "refresh"));
        record.setRedirectTo(textPayload(event.payload(), "redirectTo"));
        record.setResultText(textPayload(event.payload(), "result"));
    }

    public Criteria traceCriteria(String traceId) {
        return Criteria.of().eq("traceId", requireText(traceId, "traceId"));
    }

    public Criteria actionCriteria(String moduleAlias, String actionCode) {
        return Criteria.of()
                .eq("moduleAlias", requireText(moduleAlias, "moduleAlias"))
                .eq("actionCode", requireText(actionCode, "actionCode"));
    }

    public Criteria eventTypeCriteria(RuntimeEventType eventType) {
        if (eventType == null) {
            throw new PlatformException("Runtime audit eventType must not be null");
        }
        return Criteria.of().eq("eventType", eventType);
    }

    public Criteria recordCriteria(String moduleAlias, String entityAlias, String recordId) {
        return Criteria.of()
                .eq("moduleAlias", requireText(moduleAlias, "moduleAlias"))
                .eq("entityAlias", requireText(entityAlias, "entityAlias"))
                .eq("recordId", requireText(recordId, "recordId"));
    }

    @Override
    public void beforeInsert(RuntimeAuditRecord record) {
        if (record.getEventId() == null || record.getEventId().isBlank()) {
            throw new PlatformException("Runtime audit eventId must not be blank");
        }
        if (getDao().count(Criteria.of().eq("eventId", record.getEventId())) > 0) {
            throw new PlatformException("Runtime audit eventId must be unique: " + record.getEventId());
        }
        if (record.getTraceId() == null || record.getTraceId().isBlank()) {
            throw new PlatformException("Runtime audit traceId must not be blank");
        }
        if (record.getEventType() == null) {
            throw new PlatformException("Runtime audit eventType must not be null");
        }
        if (record.getModuleAlias() == null || record.getModuleAlias().isBlank()) {
            throw new PlatformException("Runtime audit moduleAlias must not be blank");
        }
        if (record.getSystemContext() == null) {
            record.setSystemContext(Boolean.FALSE);
        }
        if (record.getMutationSource() == null) {
            throw new PlatformException("Runtime audit mutationSource must not be null");
        }
        if (record.getOccurredAt() == null) {
            throw new PlatformException("Runtime audit occurredAt must not be null");
        }
    }

    private String payloadText(Map<String, Object> payload) {
        return payload == null || payload.isEmpty() ? null : payload.toString();
    }

    private String textPayload(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Boolean booleanPayload(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return null;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("Runtime audit " + fieldName + " must not be blank");
        }
        return value;
    }
}
