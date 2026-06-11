package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MenuService.MODULE_ALIAS, title = "平台菜单")
@RequestMapping("/platform.menu-scheme/{schemeId}/menus")
public class MenuManagementWebController extends NestedEnabledSortableCrudWebSupport<Menu, MenuService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "schemeId", "parentId", "title", "menuType", "moduleAlias", "route", "externalUrl",
            "pageMode", "defaultUiConfigId", "defaultQueryTemplateId", "enabled", "sortOrder",
            "createdAt", "updatedAt");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("title"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("schemeId", schemeId(request));
    }

    @Override
    protected void bindScope(Menu record, HttpServletRequest request) {
        record.setSchemeId(schemeId(request));
    }

    @Override
    protected boolean inScope(Menu record, HttpServletRequest request) {
        return Objects.equals(record.getSchemeId(), schemeId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "menu does not belong to scheme: " + schemeId(request) + "." + id;
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(HttpServletRequest request,
                                   @RequestParam(defaultValue = "false") boolean flat) {
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

    @GetMapping("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(HttpServletRequest request,
                                   @PathVariable String id,
                                   @RequestParam(defaultValue = "false") boolean flat,
                                   @RequestParam(defaultValue = "true") boolean includeSelf) {
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

    private String schemeId(HttpServletRequest request) {
        String value = pathVariable(request, "schemeId");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("schemeId is required");
        }
        return value;
    }
}
