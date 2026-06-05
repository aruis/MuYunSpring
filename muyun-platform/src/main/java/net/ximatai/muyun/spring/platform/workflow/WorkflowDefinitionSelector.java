package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowDefinitionSelector {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowDefinitionService definitionService;
    private final WorkflowVersionService versionService;
    private final WorkflowNodeDefinitionDao nodeDefinitionDao;
    private final WorkflowLinkDefinitionDao linkDefinitionDao;

    public WorkflowDefinitionSelector(WorkflowDefinitionService definitionService,
                                      WorkflowVersionService versionService,
                                      WorkflowNodeDefinitionDao nodeDefinitionDao,
                                      WorkflowLinkDefinitionDao linkDefinitionDao) {
        this.definitionService = definitionService;
        this.versionService = versionService;
        this.nodeDefinitionDao = nodeDefinitionDao;
        this.linkDefinitionDao = linkDefinitionDao;
    }

    public WorkflowDefinitionSelection select(WorkflowSubmitRequest request) {
        String moduleAlias = PlatformNameRules.requireModuleAlias(requireText(request.moduleAlias(), "moduleAlias"));
        Criteria criteria = Criteria.of()
                .eq("moduleAlias", moduleAlias)
                .eq("approvalEnabled", request.approvalRequired())
                .eq("definitionStatus", WorkflowDefinitionStatus.PUBLISHED)
                .eq("enabled", Boolean.TRUE);
        boolean hasDefinitionAlias = request.definitionAlias() != null && !request.definitionAlias().isBlank();
        if (hasDefinitionAlias) {
            criteria.eq("alias", PlatformNameRules.requireIdentifier(request.definitionAlias(), "workflowAlias"));
        } else if (!request.approvalRequired()) {
            throw new PlatformException("workflow definition alias is required for non-approval workflow");
        }
        List<WorkflowDefinition> definitions = definitionService.list(criteria, ALL,
                Sort.asc("sortOrder"), Sort.asc("alias"));
        if (definitions.isEmpty()) {
            throw new PlatformException("published workflow definition not found: " + moduleAlias);
        }
        if (request.approvalRequired() && !hasDefinitionAlias && definitions.size() > 1) {
            throw new PlatformException("multiple approval workflow definitions matched: " + moduleAlias);
        }
        WorkflowDefinition definition = definitions.getFirst();
        WorkflowVersion version = publishedVersion(definition);
        List<WorkflowNodeDefinition> nodes = nodeDefinitionDao.query(
                Criteria.of().eq("workflowVersionId", version.getId()), ALL, Sort.asc("sortOrder"));
        List<WorkflowLinkDefinition> links = linkDefinitionDao.query(
                Criteria.of().eq("workflowVersionId", version.getId()), ALL, Sort.asc("sortOrder"));
        return new WorkflowDefinitionSelection(definition, version, nodes, links);
    }

    private WorkflowVersion publishedVersion(WorkflowDefinition definition) {
        Criteria criteria = Criteria.of()
                .eq("definitionId", definition.getId())
                .eq("publishStatus", WorkflowPublishStatus.PUBLISHED);
        if (definition.getCurrentVersionNo() != null) {
            criteria.eq("versionNo", definition.getCurrentVersionNo());
        }
        List<WorkflowVersion> versions = versionService.list(criteria, ALL, Sort.desc("versionNo"));
        if (versions.isEmpty()) {
            throw new PlatformException("published workflow version not found: " + definition.getAlias());
        }
        return versions.getFirst();
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " must not be blank");
        }
        return value;
    }
}
