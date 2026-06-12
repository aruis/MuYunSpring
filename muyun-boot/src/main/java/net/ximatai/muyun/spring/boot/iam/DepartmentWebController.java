package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.department", title = "部门管理")
@RequestMapping("/iam.department")
public class DepartmentWebController extends WebSupport<DepartmentService> implements
        CrudWeb<Department, DepartmentService>,
        EnableWeb<Department, DepartmentService> {
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@PathVariable String id,
                                 @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            service().moveInDepartmentTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            return new WebCountResponse(1);
        });
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@RequestParam String organizationId,
                                   @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            List<Department> roots = service().rootDepartments(organizationId);
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

    @GetMapping("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@PathVariable String id,
                                   @RequestParam(defaultValue = "false") boolean flat,
                                   @RequestParam(defaultValue = "true") boolean includeSelf) {
        return webScope(() -> {
            Department root = service().select(id);
            if (root == null) {
                return new WebListResponse<>(List.of());
            }
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(treeNode(root.getOrganizationId(), root)));
                }
                return new WebListResponse<>(service().departmentChildren(root.getOrganizationId(), root.getId()).stream()
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
        for (Department child : service().departmentChildren(organizationId, parentId)) {
            rows.add(child);
            appendDescendants(organizationId, child.getId(), rows);
        }
    }

    private WebTreeNode<Department> treeNode(String organizationId, Department record) {
        return new WebTreeNode<>(WebOutputSupport.record(service(), record, FieldOutputContext.VIEW),
                service().departmentChildren(organizationId, record.getId()).stream()
                        .map(child -> treeNode(organizationId, child))
                        .toList());
    }
}
