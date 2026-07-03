package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledTreeCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = DictionaryCategoryService.MODULE_ALIAS, title = "平台数据字典类目")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "字典管理", order = 50)
@Path("/platform.dictionary_category")
public class DictionaryCategoryWebController
        extends NestedEnabledTreeCrudWebSupport<DictionaryCategory, DictionaryCategoryService>
        implements StaticModuleUiContributor {

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(DictionaryCategoryService.MODULE_ALIAS)
                .formView(form -> form
                        .title("字典类目")
                        .field("applicationAlias", field -> field.label("所属应用").required().readOnly())
                        .field("alias", field -> field.label("类目 alias").required())
                        .field("categoryKind", field -> field.label("类目类型").required().uiType("select"))
                        .field("title", field -> field.label("类目名称").required())
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }

    @Override
    protected Criteria treeScopeCriteria(WebRequestScope request) {
        String applicationAlias = applicationAlias(request);
        return applicationAlias == null ? Criteria.of() : Criteria.of().eq("applicationAlias", applicationAlias);
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        String applicationAlias = applicationAlias(request);
        if (applicationAlias != null) {
            criteria.eq("applicationAlias", applicationAlias);
        }
    }

    @Override
    protected void bindScope(DictionaryCategory record, WebRequestScope request) {
        String applicationAlias = applicationAlias(request);
        if (applicationAlias != null) {
            record.setApplicationAlias(applicationAlias);
        }
    }

    @Override
    protected boolean inScope(DictionaryCategory record, WebRequestScope request) {
        String applicationAlias = applicationAlias(request);
        return applicationAlias == null || Objects.equals(record.getApplicationAlias(), applicationAlias);
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "dictionary category does not belong to application: " + applicationAlias(request) + "." + id;
    }

    @GET
    @Path("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context UriInfo uriInfo,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat) {
        WebRequestScope request = requestScope(uriInfo);
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

    @GET
    @Path("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    public WebListResponse<?> tree(@Context UriInfo uriInfo,
                                   @PathParam("id") String id,
                                   @DefaultValue("false") @QueryParam("flat") boolean flat,
                                   @DefaultValue("true") @QueryParam("includeSelf") boolean includeSelf) {
        WebRequestScope request = requestScope(uriInfo);
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

    private String applicationAlias(WebRequestScope request) {
        String value = pathVariable(request, "applicationAlias");
        return value == null || value.isBlank() ? null : value;
    }
}
