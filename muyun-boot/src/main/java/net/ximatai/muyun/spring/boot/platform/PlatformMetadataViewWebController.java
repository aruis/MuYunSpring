package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MetadataViewService.MODULE_ALIAS, title = "平台元数据视图")
@Path("/platform.module/{moduleAlias}/metadata-relations/{relationId}/views")
public class PlatformMetadataViewWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataView, MetadataViewService> {

    private final ModuleMetadataRelationService relationService;

    public PlatformMetadataViewWebController(ModuleMetadataRelationService relationService) {
        this.relationService = relationService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        requireRelation(request);
        criteria.eq("relationId", relationId(request));
    }

    @Override
    protected void bindScope(MetadataView record, WebRequestScope request) {
        requireRelation(request);
        record.setRelationId(relationId(request));
    }

    @Override
    protected boolean inScope(MetadataView record, WebRequestScope request) {
        requireRelation(request);
        return Objects.equals(record.getRelationId(), relationId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "metadata view does not belong to relation: " + relationId(request) + "." + id;
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
