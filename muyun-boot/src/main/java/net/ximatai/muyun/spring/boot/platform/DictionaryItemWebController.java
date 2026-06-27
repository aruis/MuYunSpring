package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledTreeCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
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

@RestController
@PlatformStaticActionContribution(
        targetModule = DictionaryCategoryService.MODULE_ALIAS,
        resource = "item",
        resourceTitle = "字典项"
)
@RequestMapping({
        "/platform.dictionary_category/categories/{categoryId}/items",
        "/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items"
})
public class DictionaryItemWebController
        extends NestedEnabledTreeCrudWebSupport<DictionaryItem, DictionaryItemService> {

    @Override
    protected Criteria treeScopeCriteria(HttpServletRequest request) {
        return Criteria.of().eq("categoryId", category(request).getId());
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("categoryId", category(request).getId());
    }

    @Override
    protected void bindScope(DictionaryItem record, HttpServletRequest request) {
        DictionaryCategory category = category(request);
        record.setCategoryId(category.getId());
        record.setCategoryAlias(category.getAlias());
    }

    @Override
    protected boolean inScope(DictionaryItem record, HttpServletRequest request) {
        return Objects.equals(record.getCategoryId(), category(request).getId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "dictionary item does not belong to category: " + category(request).getId() + "." + id;
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(HttpServletRequest request,
                                   @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            DictionaryCategory category = category(request);
            List<DictionaryItem> roots = service().rootItems(category.getId());
            if (flat) {
                List<DictionaryItem> rows = new ArrayList<>();
                for (DictionaryItem root : roots) {
                    rows.add(root);
                    appendDescendants(root.getCategoryId(), root.getId(), rows);
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
                        .children(root.getCategoryId(), root.getId()).stream()
                        .map(this::node)
                        .toList());
            }
            List<DictionaryItem> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getCategoryId(), root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    private WebTreeNode<DictionaryItem> node(DictionaryItem item) {
        return new WebTreeNode<>(
                WebOutputSupport.record(service(), item, FieldOutputContext.VIEW),
                service().children(item.getCategoryId(), item.getId()).stream()
                        .map(this::node)
                        .toList());
    }

    private void appendDescendants(String categoryId, String parentId, List<DictionaryItem> rows) {
        for (DictionaryItem child : service().children(categoryId, parentId)) {
            rows.add(child);
            appendDescendants(categoryId, child.getId(), rows);
        }
    }

    private DictionaryCategory category(HttpServletRequest request) {
        String categoryId = pathVariable(request, "categoryId");
        if (categoryId != null && !categoryId.isBlank()) {
            return service().category(categoryId);
        }
        String applicationAlias = pathVariable(request, "applicationAlias");
        String categoryAlias = pathVariable(request, "categoryAlias");
        return service().category(applicationAlias, categoryAlias);
    }
}
