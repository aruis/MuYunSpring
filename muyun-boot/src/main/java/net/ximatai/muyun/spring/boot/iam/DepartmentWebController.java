package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
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
@PlatformStaticModule(application = "iam", alias = "iam.department", title = "部门管理", route = "/iam/departments")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "部门管理", order = 30)
@Path("/iam.department")
public class DepartmentWebController extends WebSupport<DepartmentService> implements
        CrudWeb<Department, DepartmentService>,
        EnableWeb<Department, DepartmentService>,
        StaticModuleUiContributor {
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    @Inject
    void setStaticRecordReadProjectionService(StaticRecordReadProjectionService staticRecordReadProjectionService) {
        this.staticRecordReadProjectionService = staticRecordReadProjectionService;
    }

    @Override
    public StaticRecordReadProjectionService staticRecordReadProjectionService() {
        return staticRecordReadProjectionService;
    }

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(DepartmentService.MODULE_ALIAS)
                .listView(list -> list
                        .title("部门列表")
                        .field("code", field -> field.label("部门编码").width("150px"))
                        .field("title", field -> field.label("部门名称").width("180px"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center")))
                .formView(form -> form
                        .title("部门档案")
                        .field("organizationId", field -> field.label("所属机构").required().readOnly())
                        .field("parentId", field -> field.label("上级部门").uiType("recordPicker"))
                        .field("code", field -> field.label("部门编码").required())
                        .field("title", field -> field.label("部门名称").required())
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }

    @POST
    @Path("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@PathParam("id") String id,
                                 TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            service().moveInDepartmentTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            return new WebCountResponse(1);
        });
    }

    @GET
    @Path("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@QueryParam("organizationId") String organizationId,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat) {
        return webScope(() -> {
            List<Department> roots = service().departmentChildrenForAction(
                    PlatformAction.TREE, organizationId, TreeAbility.ROOT_ID);
            if (flat) {
                List<Department> rows = new ArrayList<>();
                for (Department root : roots) {
                    rows.add(root);
                    appendDescendants(organizationId, root.getId(), rows);
                }
                return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
            }
            return new WebListResponse<>(roots.stream().map(root -> treeNode(organizationId, root)).toList());
        });
    }

    @GET
    @Path("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@PathParam("id") String id,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat,
                                   @DefaultValue("true") @QueryParam("includeSelf") boolean includeSelf) {
        return webScope(() -> {
            Department root = service().selectForAction(PlatformAction.TREE, id);
            if (root == null) {
                return new WebListResponse<>(List.of());
            }
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(treeNode(root.getOrganizationId(), root)));
                }
                return new WebListResponse<>(service().departmentChildrenForAction(
                                PlatformAction.TREE, root.getOrganizationId(), root.getId()).stream()
                        .map(child -> treeNode(root.getOrganizationId(), child))
                        .toList());
            }
            List<Department> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getOrganizationId(), root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    private void appendDescendants(String organizationId, String parentId, List<Department> rows) {
        for (Department child : service().departmentChildrenForAction(PlatformAction.TREE, organizationId, parentId)) {
            rows.add(child);
            appendDescendants(organizationId, child.getId(), rows);
        }
    }

    private WebTreeNode<Department> treeNode(String organizationId, Department record) {
        return new WebTreeNode<>(WebOutputSupport.record(service(), record, FieldOutputContext.VIEW),
                service().departmentChildrenForAction(PlatformAction.TREE, organizationId, record.getId()).stream()
                        .map(child -> treeNode(organizationId, child))
                        .toList());
    }
}
