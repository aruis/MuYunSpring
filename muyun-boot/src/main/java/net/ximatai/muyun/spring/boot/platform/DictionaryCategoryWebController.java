package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedEnabledTreeCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@PlatformStaticModule(application = "platform", alias = DictionaryCategoryService.MODULE_ALIAS, title = "平台数据字典类目")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "字典管理", order = 50)
@RequestMapping({"/platform.dictionary_category", "/platform.application/{applicationAlias}/dictionary-categories"})
public class DictionaryCategoryWebController
        extends NestedEnabledTreeCrudWebSupport<DictionaryCategory, DictionaryCategoryService> {
    @Override
    protected Criteria treeScopeCriteria(HttpServletRequest request) {
        String applicationAlias = applicationAlias(request);
        return applicationAlias == null ? Criteria.of() : Criteria.of().eq("applicationAlias", applicationAlias);
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        String applicationAlias = applicationAlias(request);
        if (applicationAlias != null) {
            criteria.eq("applicationAlias", applicationAlias);
        }
    }

    @Override
    protected void bindScope(DictionaryCategory record, HttpServletRequest request) {
        String applicationAlias = applicationAlias(request);
        if (applicationAlias != null) {
            record.setApplicationAlias(applicationAlias);
        }
    }

    @Override
    protected boolean inScope(DictionaryCategory record, HttpServletRequest request) {
        String applicationAlias = applicationAlias(request);
        return applicationAlias == null || Objects.equals(record.getApplicationAlias(), applicationAlias);
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "dictionary category does not belong to application: " + applicationAlias(request) + "." + id;
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(HttpServletRequest request,
                                   @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            List<DictionaryCategory> roots = applicationAlias(request) == null
                    ? service().rootCategories()
                    : service().rootCategories(applicationAlias(request));
            if (flat) {
                List<DictionaryCategory> rows = new ArrayList<>();
                for (DictionaryCategory root : roots) {
                    rows.add(root);
                    appendDescendants(root.getApplicationAlias(), root.getId(), rows);
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
            DictionaryCategory root = requireScopedRecord(request, id);
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(node(root)));
                }
                return new WebListResponse<>(service().children(root.getApplicationAlias(), root.getId()).stream()
                        .map(this::node)
                        .toList());
            }
            List<DictionaryCategory> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getApplicationAlias(), root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    private WebTreeNode<DictionaryCategory> node(DictionaryCategory category) {
        return new WebTreeNode<>(
                WebOutputSupport.record(service(), category, FieldOutputContext.VIEW),
                service().children(category.getApplicationAlias(), category.getId()).stream().map(this::node).toList());
    }

    private void appendDescendants(String applicationAlias, String parentId, List<DictionaryCategory> rows) {
        for (DictionaryCategory child : service().children(applicationAlias, parentId)) {
            rows.add(child);
            appendDescendants(applicationAlias, child.getId(), rows);
        }
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        return value == null || value.isBlank() ? null : value;
    }
}
