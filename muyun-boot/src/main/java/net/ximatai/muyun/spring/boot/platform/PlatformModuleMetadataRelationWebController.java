package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
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
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(ModuleMetadataRelation record, @Context HttpServletRequest request) {
        record.setModuleAlias(moduleAlias(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataRelation record, @Context HttpServletRequest request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "metadata relation does not belong to module: " + moduleAlias(request) + "." + id;
    }

    private String moduleAlias(@Context HttpServletRequest request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }
}
