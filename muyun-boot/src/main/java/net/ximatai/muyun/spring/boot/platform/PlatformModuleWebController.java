package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformModuleService.MODULE_ALIAS, title = "平台模块")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "模块管理", order = 20)
@Path("/platform.module")
public class PlatformModuleWebController extends WebSupport<PlatformModuleService> implements
        CrudWeb<PlatformModule, PlatformModuleService>,
        EnableWeb<PlatformModule, PlatformModuleService>,
        SystemScope<PlatformModuleService> {

    private PlatformDynamicRuntimeRefreshService runtimeRefreshService;

    @Inject
    public PlatformModuleWebController(PlatformDynamicRuntimeRefreshService runtimeRefreshService) {
        this.runtimeRefreshService = runtimeRefreshService;
    }

    public PlatformModuleWebController() {
    }

    @POST
    @Path("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@PathParam("id") String id,
                                 TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            if (blank(normalized.previousId()) && blank(normalized.nextId()) && blank(normalized.parentId())) {
                throw new IllegalArgumentException("module tree sort requires previousId, nextId, or parentId");
            }
            service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            return new WebCountResponse(1);
        });
    }

    @GET
    @Path("/tree/{applicationAlias}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@PathParam("applicationAlias") String applicationAlias,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat) {
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

    @GET
    @Path("/tree/{applicationAlias}/{parentId}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> treeChildren(@PathParam("applicationAlias") String applicationAlias,
                                           @PathParam("parentId") String parentId,
                                           @DefaultValue("false") @QueryParam("flat") boolean flat,
                                           @DefaultValue("true") @QueryParam("includeSelf") boolean includeSelf) {
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

    @POST
    @Path("/{moduleAlias}/runtime/refresh")
    @CustomActionEndpoint(value = "refreshDynamicRuntime", title = "刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult refreshRuntime(@PathParam("moduleAlias") String moduleAlias) {
        return webScope(() -> runtimeRefreshService.refresh(moduleAlias));
    }

    @POST
    @Path("/{moduleAlias}/runtime/execute-refresh")
    @CustomActionEndpoint(value = "executeRefreshDynamicRuntime", title = "执行刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult executeRefreshRuntime(@PathParam("moduleAlias") String moduleAlias) {
        return webScope(() -> runtimeRefreshService.executeRefresh(moduleAlias));
    }

    @POST
    @Path("/{moduleAlias}/runtime/preview-refresh")
    @CustomActionEndpoint(value = "previewRefreshDynamicRuntime", title = "预览刷新动态运行态",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public DynamicModuleRefreshResult previewRefreshRuntime(@PathParam("moduleAlias") String moduleAlias) {
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
