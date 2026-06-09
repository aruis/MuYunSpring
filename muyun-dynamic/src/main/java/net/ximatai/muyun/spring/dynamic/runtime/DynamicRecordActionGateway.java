package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TransactionScopeSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;

import java.util.List;
import java.util.Objects;

public class DynamicRecordActionGateway {
    private final DynamicRecordService recordService;
    private final String moduleAlias;
    private final PlatformAction action;
    private final String traceId;

    DynamicRecordActionGateway(DynamicRecordService recordService,
                               String moduleAlias,
                               PlatformAction action,
                               String traceId) {
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new IllegalArgumentException("moduleAlias must not be blank");
        }
        this.moduleAlias = moduleAlias;
        this.action = Objects.requireNonNull(action, "action must not be null");
        if (action.level() != PlatformActionLevel.LIST) {
            throw new IllegalArgumentException("dynamic record action gateway requires list-level action: "
                    + action.code());
        }
        if (action != PlatformAction.IMPORT && action != PlatformAction.EXPORT) {
            throw new IllegalArgumentException("dynamic record action gateway does not support action: "
                    + action.code());
        }
        this.traceId = traceId;
        recordService.requireAction(moduleAlias, action);
    }

    public DynamicRecord newRecord(String entityAlias) {
        return recordService.newRecord(moduleAlias, entityAlias);
    }

    public List<DynamicRecord> list(String entityAlias, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return recordService.listForAction(moduleAlias, entityAlias, action, criteria, pageRequest, sorts);
    }

    public String create(String entityAlias, DynamicRecord record) {
        requireMutationAction();
        requireActiveTransaction();
        return recordService.createFromAction(moduleAlias, entityAlias, record, traceId);
    }

    public int update(String entityAlias, DynamicRecord record) {
        requireMutationAction();
        requireActiveTransaction();
        return recordService.updateFromAction(moduleAlias, entityAlias, record, traceId);
    }

    private void requireMutationAction() {
        if (action != PlatformAction.IMPORT) {
            throw new IllegalStateException("dynamic record action gateway does not allow mutation for action: "
                    + action.code());
        }
    }

    private void requireActiveTransaction() {
        if (!TransactionScopeSupport.isTransactionActive()) {
            throw new IllegalStateException("dynamic record action gateway mutation requires active transaction: "
                    + action.code());
        }
    }
}
