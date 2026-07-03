package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformModuleActionService.MODULE_ALIAS, title = "平台模块动作")
@Path("/platform.module/{moduleAlias}/actions")
public class PlatformModuleActionWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformModuleAction, PlatformModuleActionService> {

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(PlatformModuleAction record, @Context HttpServletRequest request) {
        record.setModuleAlias(moduleAlias(request));
    }

    @Override
    protected boolean inScope(PlatformModuleAction record, @Context HttpServletRequest request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "module action does not belong to module: " + moduleAlias(request) + "." + id;
    }

    private String moduleAlias(@Context HttpServletRequest request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }
}
