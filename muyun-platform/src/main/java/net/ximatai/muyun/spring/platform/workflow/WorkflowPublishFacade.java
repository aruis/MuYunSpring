package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WorkflowPublishFacade {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowDefinitionService definitionService;
    private final WorkflowVersionService versionService;
    private final WorkflowModuleActionContributor actionContributor;
    private final WorkflowNodeDefinitionDao nodeDefinitionDao;

    public WorkflowPublishFacade(WorkflowDefinitionService definitionService,
                                 WorkflowVersionService versionService,
                                 WorkflowModuleActionContributor actionContributor) {
        this(definitionService, versionService, actionContributor, null);
    }

    @Autowired
    public WorkflowPublishFacade(WorkflowDefinitionService definitionService,
                                 WorkflowVersionService versionService,
                                 WorkflowModuleActionContributor actionContributor,
                                 WorkflowNodeDefinitionDao nodeDefinitionDao) {
        this.definitionService = definitionService;
        this.versionService = versionService;
        this.actionContributor = actionContributor;
        this.nodeDefinitionDao = nodeDefinitionDao;
    }

    @Transactional
    public WorkflowVersion publish(String definitionId, String versionId, String operatorId) {
        WorkflowDefinition definition = requireDefinition(definitionId);
        WorkflowVersion version = requireVersion(versionId);
        if (!definition.getId().equals(version.getDefinitionId())) {
            throw new PlatformException("workflow version does not belong to definition: " + versionId);
        }
        validateOvertimeDefinitions(version);
        if (version.getPublishStatus() != WorkflowPublishStatus.PUBLISHED) {
            WorkflowVersion publishing = copyVersion(version);
            publishing.setPublishStatus(WorkflowPublishStatus.PUBLISHED);
            publishing.setPublishedBy(operatorId == null || operatorId.isBlank() ? null : operatorId);
            publishing.setPublishedAt(Instant.now());
            versionService.update(publishing);
            version = publishing;
        }
        definition.setDefinitionStatus(WorkflowDefinitionStatus.PUBLISHED);
        definition.setCurrentVersionNo(version.getVersionNo());
        definitionService.update(definition);
        actionContributor.registerPublishedWorkflowAction(definition, version);
        return version;
    }

    public WorkflowVersion publish(String definitionId, String versionId) {
        return publish(definitionId, versionId, null);
    }

    @Transactional
    public WorkflowDefinition disable(String definitionId) {
        return changeDefinitionStatus(definitionId, WorkflowDefinitionStatus.DISABLED);
    }

    @Transactional
    public WorkflowDefinition archive(String definitionId) {
        return changeDefinitionStatus(definitionId, WorkflowDefinitionStatus.ARCHIVED);
    }

    private WorkflowDefinition changeDefinitionStatus(String definitionId, WorkflowDefinitionStatus status) {
        WorkflowDefinition definition = requireDefinition(definitionId);
        definition.setDefinitionStatus(status);
        definitionService.update(definition);
        actionContributor.disableWorkflowActions(definition);
        return definition;
    }

    private WorkflowDefinition requireDefinition(String definitionId) {
        WorkflowDefinition definition = definitionService.select(definitionId);
        if (definition == null) {
            throw new PlatformException("workflow definition not found: " + definitionId);
        }
        return definition;
    }

    private WorkflowVersion requireVersion(String versionId) {
        WorkflowVersion version = versionService.select(versionId);
        if (version == null) {
            throw new PlatformException("workflow version not found: " + versionId);
        }
        return version;
    }

    private void validateOvertimeDefinitions(WorkflowVersion version) {
        if (nodeDefinitionDao == null) {
            return;
        }
        List<WorkflowNodeDefinition> nodes = nodeDefinitionDao.query(
                Criteria.of().eq("workflowVersionId", version.getId()), ALL);
        for (WorkflowNodeDefinition node : nodes) {
            validateOvertimeDefinition(node);
        }
    }

    private void validateOvertimeDefinition(WorkflowNodeDefinition node) {
        validatePositiveDuration(node.getWarningDurationMinutes(), "warningDurationMinutes", node);
        validatePositiveDuration(node.getOvertimeDurationMinutes(), "overtimeDurationMinutes", node);
        if (node.getWarningDurationMinutes() != null
                && node.getOvertimeDurationMinutes() != null
                && node.getOvertimeDurationMinutes() < node.getWarningDurationMinutes()) {
            throw new PlatformException("workflow overtimeDurationMinutes must not be less than warningDurationMinutes: "
                    + node.getNodeKey());
        }
        if (node.getNodeType() != WorkflowNodeType.APPROVAL
                && (node.getWarningDurationMinutes() != null || node.getOvertimeDurationMinutes() != null)) {
            throw new PlatformException("workflow overtime durations are only supported on approval nodes: "
                    + node.getNodeKey());
        }
    }

    private void validatePositiveDuration(Integer value, String fieldName, WorkflowNodeDefinition node) {
        if (value != null && value <= 0) {
            throw new PlatformException("workflow " + fieldName + " must be positive: " + node.getNodeKey());
        }
    }

    private WorkflowVersion copyVersion(WorkflowVersion source) {
        WorkflowVersion copy = new WorkflowVersion();
        copy.setId(source.getId());
        copy.setTenantId(source.getTenantId());
        copy.setVersion(source.getVersion());
        copy.setDeleted(source.getDeleted());
        copy.setDeletedAt(source.getDeletedAt());
        copy.setDefinitionId(source.getDefinitionId());
        copy.setVersionNo(source.getVersionNo());
        copy.setPublishStatus(source.getPublishStatus());
        copy.setSnapshotText(source.getSnapshotText());
        copy.setPublishedBy(source.getPublishedBy());
        copy.setPublishedAt(source.getPublishedAt());
        return copy;
    }
}
