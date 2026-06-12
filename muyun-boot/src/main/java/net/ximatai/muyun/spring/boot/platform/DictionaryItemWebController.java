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
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
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
@PlatformStaticModule(application = "platform", alias = DictionaryItemService.MODULE_ALIAS, title = "平台数据字典项目")
@RequestMapping("/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items")
public class DictionaryItemWebController
        extends NestedEnabledSortableCrudWebSupport<DictionaryItem, DictionaryItemService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "applicationAlias", "categoryAlias", "code", "parentId", "title",
            "enabled", "sortOrder", "createdAt", "updatedAt");

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
        criteria.eq("applicationAlias", applicationAlias(request));
        criteria.eq("categoryAlias", categoryAlias(request));
    }

    @Override
    protected void bindScope(DictionaryItem record, HttpServletRequest request) {
        record.setApplicationAlias(applicationAlias(request));
        record.setCategoryAlias(categoryAlias(request));
    }

    @Override
    protected boolean inScope(DictionaryItem record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request))
                && Objects.equals(record.getCategoryAlias(), categoryAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "dictionary item does not belong to category: "
                + applicationAlias(request) + "." + categoryAlias(request) + "." + id;
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(HttpServletRequest request,
                                   @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            List<DictionaryItem> roots = service().rootItems(applicationAlias(request), categoryAlias(request));
            if (flat) {
                List<DictionaryItem> rows = new ArrayList<>();
                for (DictionaryItem root : roots) {
                    rows.add(root);
                    appendDescendants(root.getApplicationAlias(), root.getCategoryAlias(), root.getId(), rows);
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
            DictionaryItem root = requireScopedRecord(request, id);
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(node(root)));
                }
                return new WebListResponse<>(service()
                        .children(applicationAlias(request), categoryAlias(request), root.getId()).stream()
                        .map(this::node)
                        .toList());
            }
            List<DictionaryItem> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getApplicationAlias(), root.getCategoryAlias(), root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    private WebTreeNode<DictionaryItem> node(DictionaryItem item) {
        return new WebTreeNode<>(
                WebOutputSupport.record(service(), item, FieldOutputContext.VIEW),
                service().children(item.getApplicationAlias(), item.getCategoryAlias(), item.getId()).stream()
                        .map(this::node)
                        .toList());
    }

    private void appendDescendants(String applicationAlias, String categoryAlias, String parentId,
                                   List<DictionaryItem> rows) {
        for (DictionaryItem child : service().children(applicationAlias, categoryAlias, parentId)) {
            rows.add(child);
            appendDescendants(applicationAlias, categoryAlias, child.getId(), rows);
        }
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
    }

    private String categoryAlias(HttpServletRequest request) {
        String value = pathVariable(request, "categoryAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("categoryAlias is required");
        }
        return value;
    }
}
