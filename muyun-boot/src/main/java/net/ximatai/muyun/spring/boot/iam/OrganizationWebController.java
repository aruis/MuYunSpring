package net.ximatai.muyun.spring.boot.iam;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.organization", title = "机构管理", route = "/iam/organizations")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 20)
@RequestMapping("/iam.organization")
public class OrganizationWebController extends WebSupport<OrganizationService> implements
        CrudWeb<Organization, OrganizationService>,
        EnableWeb<Organization, OrganizationService>,
        TreeWeb<Organization, OrganizationService> {
    @Override
    public List<Organization> treeChildren(HttpServletRequest request, String parentId) {
        return service().organizationChildrenForAction(
                PlatformAction.TREE, resolveTreeTenantId(request.getParameter("tenantId")), parentId);
    }

    @Override
    public Organization treeSelect(HttpServletRequest request, String id) {
        return service().organizationForAction(
                PlatformAction.TREE, resolveTreeTenantId(request.getParameter("tenantId")), id);
    }

    @Override
    public List<Organization> treeSortChildren(HttpServletRequest request, String parentId) {
        return service().organizationChildrenForAction(
                PlatformAction.SORT, resolveTreeTenantId(request.getParameter("tenantId")), parentId);
    }

    @Override
    public Organization treeSortSelect(HttpServletRequest request, String id) {
        return service().organizationForAction(
                PlatformAction.SORT, resolveTreeTenantId(request.getParameter("tenantId")), id);
    }

    @Override
    public void moveTree(HttpServletRequest request, String id, TreeSortWebRequest sortRequest) {
        service().moveInOrganizationTree(resolveTreeTenantId(request.getParameter("tenantId")),
                id, sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId());
    }

    private String resolveTreeTenantId(String requestedTenantId) {
        String normalized = requestedTenantId == null || requestedTenantId.isBlank()
                ? null
                : requestedTenantId.trim();
        if (TenantContext.isSystem()) {
            return normalized;
        }
        String currentTenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("iam.organization tree requires tenant context"));
        if (normalized != null && !currentTenantId.equals(normalized)) {
            throw new PlatformException("organization tree tenantId must match current tenant");
        }
        return currentTenantId;
    }
}
