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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@PlatformStaticActionContribution(
        targetModule = DictionaryCategoryService.MODULE_ALIAS,
        resource = "item",
        resourceTitle = "字典项"
)
@Path("/platform.dictionary_category/categories/{categoryId}/items")
public class DictionaryItemWebController
        extends NestedEnabledTreeCrudWebSupport<DictionaryItem, DictionaryItemService>
        implements StaticModuleUiContributor {

    private static final String RESOURCE = "item";

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(DictionaryCategoryService.MODULE_ALIAS)
                .formView(ModuleUiViewCodes.childResourceDefaultForm(RESOURCE), form -> form
                        .title("字典项")
                        .field(RESOURCE, "categoryId", field -> field.label("所属类目").readOnly())
                        .field(RESOURCE, "code", field -> field.label("字典项编码").required())
                        .field(RESOURCE, "title", field -> field.label("字典项名称").required())
                        .field(RESOURCE, "parentId", field -> field.label("上级字典项").uiType("recordPicker"))
                        .field(RESOURCE, "enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }

    @Override
    protected Criteria treeScopeCriteria(@Context HttpServletRequest request) {
        return Criteria.of().eq("categoryId", category(request).getId());
    }

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("categoryId", category(request).getId());
    }

    @Override
    protected void bindScope(DictionaryItem record, @Context HttpServletRequest request) {
        DictionaryCategory category = category(request);
        record.setCategoryId(category.getId());
        record.setCategoryAlias(category.getAlias());
    }

    @Override
    protected boolean inScope(DictionaryItem record, @Context HttpServletRequest request) {
        return Objects.equals(record.getCategoryId(), category(request).getId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "dictionary item does not belong to category: " + category(request).getId() + "." + id;
    }

    @GET
    @Path("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context HttpServletRequest request,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat) {
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

    @GET
    @Path("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context HttpServletRequest request,
                                   @PathParam("id") String id,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat,
                                   @DefaultValue("true") @QueryParam("includeSelf") boolean includeSelf) {
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

    private DictionaryCategory category(@Context HttpServletRequest request) {
        String categoryId = pathVariable(request, "categoryId");
        if (categoryId != null && !categoryId.isBlank()) {
            return service().category(categoryId);
        }
        String applicationAlias = pathVariable(request, "applicationAlias");
        String categoryAlias = pathVariable(request, "categoryAlias");
        return service().category(applicationAlias, categoryAlias);
    }
}
