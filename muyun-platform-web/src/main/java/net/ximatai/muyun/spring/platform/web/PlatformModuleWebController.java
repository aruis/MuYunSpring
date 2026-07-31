package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.web.StandardMutation;
import net.ximatai.muyun.spring.web.StandardMutationKind;
import net.ximatai.muyun.spring.web.StandardMutationResultSupport;
import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.web.WebListResponse;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRefreshResult;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = PlatformModuleService.MODULE_ALIAS, title = "平台模块")
@PlatformMenu(parent = PlatformMenuGroups.MODELING, title = "模块管理", order = 20)
@RequestMapping("/platform.module")
public class PlatformModuleWebController extends WebSupport<PlatformModuleService> implements
        CrudWeb<PlatformModule, PlatformModuleService>,
        SystemScope<PlatformModuleService> {

    private PlatformDynamicRuntimeRefreshService runtimeRefreshService;

    @Autowired
    public PlatformModuleWebController(PlatformDynamicRuntimeRefreshService runtimeRefreshService) {
        this.runtimeRefreshService = runtimeRefreshService;
    }

    public PlatformModuleWebController() {
    }

    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    @StandardMutation(StandardMutationKind.SORT)
    public int sort(@PathVariable String id,
                    @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            if (blank(normalized.previousId()) && blank(normalized.nextId()) && blank(normalized.parentId())) {
                throw new IllegalArgumentException("module tree sort requires previousId, nextId, or parentId");
            }
            return StandardMutationResultSupport.sorted(this, () -> {
                service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
                return 1;
            });
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

    @PostMapping("/{moduleAlias}/runtime/refresh")
    @CustomActionEndpoint(value = "refreshDynamicRuntime", title = "刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult refreshRuntime(@PathVariable String moduleAlias) {
        return webScope(() -> runtimeRefreshService.refresh(moduleAlias));
    }

    @PostMapping("/{moduleAlias}/runtime/execute-refresh")
    @CustomActionEndpoint(value = "executeRefreshDynamicRuntime", title = "执行刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult executeRefreshRuntime(@PathVariable String moduleAlias) {
        return webScope(() -> runtimeRefreshService.executeRefresh(moduleAlias));
    }

    @PostMapping("/{moduleAlias}/runtime/preview-refresh")
    @CustomActionEndpoint(value = "previewRefreshDynamicRuntime", title = "预览刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult previewRefreshRuntime(@PathVariable String moduleAlias) {
        return webScope(() -> runtimeRefreshService.previewRefresh(moduleAlias));
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
