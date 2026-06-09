package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeIssueLogService extends AbstractAbilityService<CodeIssueLog> implements
        SoftDeleteAbility<CodeIssueLog> {
    public static final String MODULE_ALIAS = "platform.code_issue_log";

    public CodeIssueLogService(BaseDao<CodeIssueLog, String> issueLogDao) {
        super(MODULE_ALIAS, CodeIssueLog.class, issueLogDao);
    }

    public List<CodeIssueLog> selectByRuleId(String ruleId, int limit) {
        if (ruleId == null || ruleId.isBlank()) {
            return List.of();
        }
        return list(Criteria.of().eq("ruleId", ruleId), PageRequest.of(1, Math.max(1, limit)),
                Sort.desc("createdAt"));
    }

    public CodeIssueLog write(CodeRule rule,
                              String basisKey,
                              String periodKey,
                              String generatedValue,
                              CodeIssueLogStatus status,
                              Integer retryCount,
                              String message) {
        if (rule == null || rule.getId() == null || rule.getId().isBlank()) {
            return null;
        }
        CodeIssueLog log = new CodeIssueLog();
        log.setRuleId(rule.getId());
        log.setModuleAlias(rule.getModuleAlias());
        log.setEntityAlias(rule.getEntityAlias());
        log.setFieldName(rule.getFieldName());
        log.setBasisKey(normalizeBucket(basisKey));
        log.setPeriodKey(normalizeBucket(periodKey));
        log.setGeneratedValue(generatedValue);
        log.setStatus(status == null ? CodeIssueLogStatus.SUCCESS : status);
        log.setRetryCount(retryCount == null ? 0 : retryCount);
        log.setMessage(message);
        insert(log);
        return log;
    }

    @Override
    public void beforeInsert(CodeIssueLog log) {
        normalizeAndValidate(log);
    }

    @Override
    public void beforeUpdate(CodeIssueLog log) {
        normalizeAndValidate(log);
    }

    private void normalizeAndValidate(CodeIssueLog log) {
        if (log.getRuleId() == null || log.getRuleId().isBlank()) {
            throw new PlatformException("Code issue log requires ruleId");
        }
        if (log.getModuleAlias() == null || log.getModuleAlias().isBlank()) {
            throw new PlatformException("Code issue log requires moduleAlias");
        }
        if (log.getEntityAlias() == null || log.getEntityAlias().isBlank()) {
            throw new PlatformException("Code issue log requires entityAlias");
        }
        if (log.getFieldName() == null || log.getFieldName().isBlank()) {
            throw new PlatformException("Code issue log requires fieldName");
        }
        log.setBasisKey(normalizeBucket(log.getBasisKey()));
        log.setPeriodKey(normalizeBucket(log.getPeriodKey()));
        if (log.getStatus() == null) {
            log.setStatus(CodeIssueLogStatus.SUCCESS);
        }
        if (log.getRetryCount() == null) {
            log.setRetryCount(0);
        }
    }

    private String normalizeBucket(String value) {
        return value == null || value.isBlank() ? CodeSequenceState.DEFAULT_BUCKET : value;
    }
}
