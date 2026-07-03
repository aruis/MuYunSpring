package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = ModuleMetadataRelationService.MODULE_ALIAS, title = "平台模块元数据关系")
@Path("/platform.module/{moduleAlias}/metadata-relations")
public class PlatformModuleMetadataRelationWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataRelation, ModuleMetadataRelationService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(ModuleMetadataRelation record, WebRequestScope request) {
        record.setModuleAlias(moduleAlias(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataRelation record, WebRequestScope request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "metadata relation does not belong to module: " + moduleAlias(request) + "." + id;
    }

    private String moduleAlias(WebRequestScope request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }
}
