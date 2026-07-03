package net.ximatai.muyun.spring.boot.platform;

import jakarta.enterprise.context.ApplicationScoped;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MenuService.MODULE_ALIAS, title = "平台菜单")
@Path("/platform.menu-scheme/{schemeId}/menus")
public class MenuManagementWebController extends NestedEnabledSortableCrudWebSupport<Menu, MenuService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("schemeId", schemeId(request));
    }

    @Override
    protected void bindScope(Menu record, WebRequestScope request) {
        record.setSchemeId(schemeId(request));
    }

    @Override
    protected boolean inScope(Menu record, WebRequestScope request) {
        return Objects.equals(record.getSchemeId(), schemeId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "menu does not belong to scheme: " + schemeId(request) + "." + id;
    }

    @GET
    @Path("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context UriInfo uriInfo,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> {
            List<Menu> roots = service().rootMenus(schemeId(request));
            if (flat) {
                List<Menu> rows = new ArrayList<>();
                for (Menu root : roots) {
                    rows.add(root);
                    appendDescendants(root.getSchemeId(), root.getId(), rows);
                }
                return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
            }
            return new WebListResponse<>(roots.stream().map(this::node).toList());
        });
    }

    @GET
    @Path("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context UriInfo uriInfo,
                                   @PathParam("id") String id,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat,
                                   @DefaultValue("true") @QueryParam("includeSelf") boolean includeSelf) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> {
            Menu root = requireScopedRecord(request, id);
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(node(root)));
                }
                return new WebListResponse<>(service().children(schemeId(request), root.getId()).stream()
                        .map(this::node)
                        .toList());
            }
            List<Menu> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getSchemeId(), root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    private WebTreeNode<Menu> node(Menu menu) {
        return new WebTreeNode<>(
                WebOutputSupport.record(service(), menu, FieldOutputContext.VIEW),
                service().children(menu.getSchemeId(), menu.getId()).stream().map(this::node).toList());
    }

    private void appendDescendants(String schemeId, String parentId, List<Menu> rows) {
        for (Menu child : service().children(schemeId, parentId)) {
            rows.add(child);
            appendDescendants(schemeId, child.getId(), rows);
        }
    }

    private String schemeId(WebRequestScope request) {
        String value = pathVariable(request, "schemeId");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("schemeId is required");
        }
        return value;
    }
}
