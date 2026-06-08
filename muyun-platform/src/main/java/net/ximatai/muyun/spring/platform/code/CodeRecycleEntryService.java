package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeRecycleEntryService extends AbstractAbilityService<CodeRecycleEntry> implements
        SoftDeleteAbility<CodeRecycleEntry> {
    public static final String MODULE_ALIAS = "platform.code_recycle_entry";
    private static final PageRequest ONE = PageRequest.of(1, 1);

    private final CodeRecycleConsumer recycleConsumer;

    public CodeRecycleEntryService(BaseDao<CodeRecycleEntry, String> recycleEntryDao) {
        this(recycleEntryDao, List.of());
    }

    @Autowired
    public CodeRecycleEntryService(BaseDao<CodeRecycleEntry, String> recycleEntryDao,
                                   List<CodeRecycleConsumer> recycleConsumers) {
        super(MODULE_ALIAS, CodeRecycleEntry.class, recycleEntryDao);
        if (recycleConsumers != null && recycleConsumers.size() > 1) {
            throw new PlatformException("Only one CodeRecycleConsumer can be active");
        }
        this.recycleConsumer = recycleConsumers == null || recycleConsumers.isEmpty()
                ? null
                : recycleConsumers.getFirst();
    }

    public synchronized CodeRecycleEntry consumeAvailable(String ruleId, String basisKey, String periodKey) {
        String effectiveBasisKey = normalizeBucket(basisKey);
        String effectivePeriodKey = normalizeBucket(periodKey);
        if (recycleConsumer != null) {
            return recycleConsumer.consumeAvailable(ruleId, effectiveBasisKey, effectivePeriodKey,
                    TenantContext.currentTenantId().orElse(null));
        }
        List<CodeRecycleEntry> entries = list(Criteria.of()
                .eq("ruleId", ruleId)
                .eq("basisKey", effectiveBasisKey)
                .eq("periodKey", effectivePeriodKey)
                .eq("status", CodeRecycleStatus.AVAILABLE), ONE);
        if (entries.isEmpty()) {
            return null;
        }
        CodeRecycleEntry entry = entries.getFirst();
        entry.setStatus(CodeRecycleStatus.USED);
        update(entry);
        return entry;
    }

    public CodeRecycleEntry record(CodeRule rule,
                                   String basisKey,
                                   String periodKey,
                                   String recycledValue,
                                   String sourceRecordId) {
        String effectiveBasisKey = normalizeBucket(basisKey);
        String effectivePeriodKey = normalizeBucket(periodKey);
        CodeRecycleEntry entry = findOne(Criteria.of()
                .eq("ruleId", rule.getId())
                .eq("basisKey", effectiveBasisKey)
                .eq("periodKey", effectivePeriodKey)
                .eq("recycledValue", recycledValue));
        if (entry == null) {
            entry = new CodeRecycleEntry();
            entry.setRuleId(rule.getId());
            entry.setBasisKey(effectiveBasisKey);
            entry.setPeriodKey(effectivePeriodKey);
            entry.setRecycledValue(recycledValue);
        }
        entry.setSourceRecordId(sourceRecordId);
        entry.setStatus(Boolean.TRUE.equals(rule.getAllowRecycle())
                ? CodeRecycleStatus.AVAILABLE
                : CodeRecycleStatus.DISCARDED);
        if (entry.getId() == null) {
            insert(entry);
        } else {
            update(entry);
        }
        return entry;
    }

    @Override
    public void beforeInsert(CodeRecycleEntry entry) {
        normalizeAndValidate(entry);
    }

    @Override
    public void beforeUpdate(CodeRecycleEntry entry) {
        normalizeAndValidate(entry);
    }

    private void normalizeAndValidate(CodeRecycleEntry entry) {
        if (entry.getRuleId() == null || entry.getRuleId().isBlank()) {
            throw new PlatformException("Code recycle entry requires ruleId");
        }
        if (entry.getRecycledValue() == null || entry.getRecycledValue().isBlank()) {
            throw new PlatformException("Code recycle entry requires recycledValue");
        }
        entry.setBasisKey(normalizeBucket(entry.getBasisKey()));
        entry.setPeriodKey(normalizeBucket(entry.getPeriodKey()));
        if (entry.getStatus() == null) {
            entry.setStatus(CodeRecycleStatus.AVAILABLE);
        }
    }

    private String normalizeBucket(String value) {
        return value == null || value.isBlank() ? CodeSequenceState.DEFAULT_BUCKET : value;
    }
}
