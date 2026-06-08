package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CodeLedgerEntryService extends AbstractAbilityService<CodeLedgerEntry> implements
        SoftDeleteAbility<CodeLedgerEntry> {
    public static final String MODULE_ALIAS = "platform.code_ledger_entry";

    public CodeLedgerEntryService(BaseDao<CodeLedgerEntry, String> ledgerEntryDao) {
        super(MODULE_ALIAS, CodeLedgerEntry.class, ledgerEntryDao);
    }

    public synchronized CodeLedgerEntry upsertActiveBinding(CodeRule rule,
                                                            String codeValue,
                                                            String basisKey,
                                                            String periodKey,
                                                            String sourceRecordId) {
        CodeLedgerEntry entry = findByRuleAndValue(rule.getId(), codeValue);
        if (entry == null) {
            entry = new CodeLedgerEntry();
            entry.setRuleId(rule.getId());
            entry.setCodeValue(codeValue);
            entry.setModuleAlias(rule.getModuleAlias());
            entry.setEntityAlias(rule.getEntityAlias());
            entry.setFieldName(rule.getFieldName());
        } else if (entry.getStatus() == CodeLedgerStatus.ACTIVE
                && !Objects.equals(entry.getSourceRecordId(), sourceRecordId)) {
            throw new PlatformException("Code value is already occupied: " + codeValue);
        }
        entry.setBasisKey(normalizeBucket(basisKey));
        entry.setPeriodKey(normalizeBucket(periodKey));
        entry.setSourceRecordId(sourceRecordId);
        entry.setStatus(CodeLedgerStatus.ACTIVE);
        entry.setLastAction(CodeLedgerAction.ASSIGNED);
        if (entry.getId() == null) {
            insert(entry);
        } else {
            update(entry);
        }
        return entry;
    }

    public synchronized CodeLedgerEntry upsertInactiveBinding(CodeRule rule,
                                                              String codeValue,
                                                              String basisKey,
                                                              String periodKey,
                                                              String sourceRecordId,
                                                              CodeLedgerStatus status,
                                                              CodeLedgerAction action) {
        CodeLedgerEntry entry = findByRuleAndValue(rule.getId(), codeValue);
        if (entry == null) {
            entry = new CodeLedgerEntry();
            entry.setRuleId(rule.getId());
            entry.setCodeValue(codeValue);
            entry.setModuleAlias(rule.getModuleAlias());
            entry.setEntityAlias(rule.getEntityAlias());
            entry.setFieldName(rule.getFieldName());
        }
        entry.setBasisKey(normalizeBucket(basisKey));
        entry.setPeriodKey(normalizeBucket(periodKey));
        entry.setSourceRecordId(null);
        entry.setStatus(status == null ? CodeLedgerStatus.DISCARDED : status);
        entry.setLastAction(action == null ? CodeLedgerAction.RELEASED_BY_DELETE : action);
        if (entry.getId() == null) {
            insert(entry);
        } else {
            update(entry);
        }
        return entry;
    }

    public CodeLedgerEntry findByRuleAndValue(String ruleId, String codeValue) {
        if (ruleId == null || ruleId.isBlank() || codeValue == null || codeValue.isBlank()) {
            return null;
        }
        return findOne(Criteria.of()
                .eq("ruleId", ruleId)
                .eq("codeValue", codeValue));
    }

    @Override
    public void beforeInsert(CodeLedgerEntry entry) {
        normalizeAndValidate(entry);
        rejectDuplicate(entry, Criteria.of()
                        .eq("ruleId", entry.getRuleId())
                        .eq("codeValue", entry.getCodeValue()),
                "Code ledger entry already exists for value: " + entry.getRuleId() + "/" + entry.getCodeValue());
    }

    @Override
    public void beforeUpdate(CodeLedgerEntry entry) {
        normalizeAndValidate(entry);
        rejectDuplicate(entry, Criteria.of()
                        .eq("ruleId", entry.getRuleId())
                        .eq("codeValue", entry.getCodeValue()),
                "Code ledger entry already exists for value: " + entry.getRuleId() + "/" + entry.getCodeValue());
    }

    private void normalizeAndValidate(CodeLedgerEntry entry) {
        if (entry.getRuleId() == null || entry.getRuleId().isBlank()) {
            throw new PlatformException("Code ledger entry requires ruleId");
        }
        if (entry.getCodeValue() == null || entry.getCodeValue().isBlank()) {
            throw new PlatformException("Code ledger entry requires codeValue");
        }
        if (entry.getModuleAlias() == null || entry.getModuleAlias().isBlank()) {
            throw new PlatformException("Code ledger entry requires moduleAlias");
        }
        if (entry.getEntityAlias() == null || entry.getEntityAlias().isBlank()) {
            throw new PlatformException("Code ledger entry requires entityAlias");
        }
        if (entry.getFieldName() == null || entry.getFieldName().isBlank()) {
            throw new PlatformException("Code ledger entry requires fieldName");
        }
        entry.setBasisKey(normalizeBucket(entry.getBasisKey()));
        entry.setPeriodKey(normalizeBucket(entry.getPeriodKey()));
        if (entry.getStatus() == null) {
            entry.setStatus(CodeLedgerStatus.ACTIVE);
        }
        if (entry.getLastAction() == null) {
            entry.setLastAction(CodeLedgerAction.ASSIGNED);
        }
    }

    private String normalizeBucket(String value) {
        return value == null || value.isBlank() ? CodeSequenceState.DEFAULT_BUCKET : value;
    }
}
