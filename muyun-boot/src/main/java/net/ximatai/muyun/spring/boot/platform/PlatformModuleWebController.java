package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformModuleService.MODULE_ALIAS, title = "平台模块")
@RequestMapping("/platform.module")
public class PlatformModuleWebController extends WebSupport<PlatformModuleService> implements
        CrudWeb<PlatformModule, PlatformModuleService>,
        EnableWeb<PlatformModule, PlatformModuleService>,
        SystemScope<PlatformModuleService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "parentId", "applicationAlias", "moduleKind", "systemManaged",
            "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }

    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@PathVariable String id,
                                 @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            if (blank(normalized.previousId()) && blank(normalized.nextId()) && blank(normalized.parentId())) {
                throw new IllegalArgumentException("module tree sort requires previousId, nextId, or parentId");
            }
            service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            return new WebCountResponse(1);
        });
    }

    @GetMapping("/tree/{applicationAlias}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@PathVariable String applicationAlias,
                                   @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
            List<PlatformModule> roots = service().rootModules(validApplicationAlias);
            if (flat) {
                List<PlatformModule> rows = new ArrayList<>();
                for (PlatformModule root : roots) {
                    rows.add(root);
                    appendDescendants(validApplicationAlias, root.getId(), rows);
                }
                return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
            }
            return new WebListResponse<>(roots.stream()
                    .map(root -> treeNode(validApplicationAlias, root))
                    .toList());
        });
    }

    @GetMapping("/tree/{applicationAlias}/{parentId}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> treeChildren(@PathVariable String applicationAlias,
                                           @PathVariable String parentId,
                                           @RequestParam(defaultValue = "false") boolean flat,
                                           @RequestParam(defaultValue = "true") boolean includeSelf) {
        return webScope(() -> {
            String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
            PlatformModule root = TreeAbility.ROOT_ID.equals(parentId) ? null : service().select(parentId);
            if (root != null && !validApplicationAlias.equals(root.getApplicationAlias())) {
                throw new IllegalArgumentException("module parent must belong to application: " + validApplicationAlias);
            }
            if (flat) {
                List<PlatformModule> rows = new ArrayList<>();
                if (includeSelf && root != null) {
                    rows.add(root);
                }
                appendDescendants(validApplicationAlias, parentId, rows);
                return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
            }
            if (includeSelf && root != null) {
                return new WebListResponse<>(List.of(treeNode(validApplicationAlias, root)));
            }
            return new WebListResponse<>(service().children(validApplicationAlias, parentId).stream()
                    .map(child -> treeNode(validApplicationAlias, child))
                    .toList());
        });
    }

    private void appendDescendants(String applicationAlias, String parentId, List<PlatformModule> rows) {
        for (PlatformModule child : service().children(applicationAlias, parentId)) {
            rows.add(child);
            appendDescendants(applicationAlias, child.getId(), rows);
        }
    }

    private WebTreeNode<PlatformModule> treeNode(String applicationAlias, PlatformModule record) {
        return new WebTreeNode<>(
                WebOutputSupport.record(service(), record, FieldOutputContext.VIEW),
                service().children(applicationAlias, record.getId()).stream()
                        .map(child -> treeNode(applicationAlias, child))
                        .toList());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
