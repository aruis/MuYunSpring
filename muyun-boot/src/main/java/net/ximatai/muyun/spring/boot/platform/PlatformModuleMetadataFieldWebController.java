package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = ModuleMetadataFieldService.MODULE_ALIAS, title = "平台模块字段配置")
@Path("/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields")
public class PlatformModuleMetadataFieldWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataField, ModuleMetadataFieldService> {

    private final ModuleMetadataRelationService relationService;

    public PlatformModuleMetadataFieldWebController(ModuleMetadataRelationService relationService) {
        this.relationService = relationService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        requireRelation(request);
        criteria.eq("relationId", relationId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataField record, WebRequestScope request) {
        requireRelation(request);
        record.setRelationId(relationId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataField record, WebRequestScope request) {
        requireRelation(request);
        return relationId(request).equals(record.getRelationId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "module metadata field does not belong to relation: " + relationId(request) + "." + id;
    }

    @POST
    @Path("/ensure")
    @CustomActionEndpoint(value = "ensureFields", title = "同步模块字段配置",
            level = PlatformActionLevel.LIST, dataAuth = false)
    public WebListResponse<ModuleMetadataField> ensure(@Context UriInfo uriInfo) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> {
            requireRelation(request);
            return new WebListResponse<>(WebOutputSupport.records(service(),
                    service().ensureForRelation(relationId(request)), FieldOutputContext.VIEW));
        });
    }

    private ModuleMetadataRelation requireRelation(WebRequestScope request) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
        ModuleMetadataRelation relation = relationService.select(relationId(request));
        if (relation == null || !validModuleAlias.equals(relation.getModuleAlias())) {
            throw new IllegalArgumentException("metadata relation does not belong to module: "
                    + validModuleAlias + "." + relationId(request));
        }
        return relation;
    }

    private String relationId(WebRequestScope request) {
        return pathVariable(request, "relationId");
    }
}
