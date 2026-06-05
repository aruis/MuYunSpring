package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowVersionService extends AbstractAbilityService<WorkflowVersion> implements
        SoftDeleteAbility<WorkflowVersion> {
    public static final String MODULE_ALIAS = "platform.workflow.version";

    public WorkflowVersionService(BaseDao<WorkflowVersion, String> workflowVersionDao,
                                  WorkflowDefinitionService definitionService) {
        super(MODULE_ALIAS, WorkflowVersion.class, workflowVersionDao);
        this.definitionService = definitionService;
    }

    private final WorkflowDefinitionService definitionService;

    @Override
    public void beforeInsert(WorkflowVersion version) {
        normalizeAndValidate(version);
    }

    @Override
    public void beforeUpdate(WorkflowVersion version) {
        WorkflowVersion existing = selectIncludingDeleted(version.getId());
        if (existing != null && existing.getPublishStatus() == WorkflowPublishStatus.PUBLISHED) {
            throw new PlatformException("published workflow version cannot be changed");
        }
        normalizeAndValidate(version);
    }

    private void normalizeAndValidate(WorkflowVersion version) {
        if (version.getDefinitionId() == null || version.getDefinitionId().isBlank()) {
            throw new PlatformException("workflow definition id must not be blank");
        }
        if (definitionService.select(version.getDefinitionId()) == null) {
            throw new PlatformException("workflow definition not found: " + version.getDefinitionId());
        }
        if (version.getVersionNo() == null || version.getVersionNo() <= 0) {
            throw new PlatformException("workflow version number must be positive");
        }
        if (version.getPublishStatus() == null) {
            version.setPublishStatus(WorkflowPublishStatus.DRAFT);
        }
        rejectDuplicate(version, Criteria.of()
                        .eq("definitionId", version.getDefinitionId())
                        .eq("versionNo", version.getVersionNo()),
                "workflow version number must be unique within definition: " + version.getVersionNo());
    }
}
