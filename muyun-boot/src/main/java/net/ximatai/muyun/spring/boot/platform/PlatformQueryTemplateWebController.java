package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformQueryTemplateService.MODULE_ALIAS,
        title = "平台查询模板")
@Path("/platform.module/{moduleAlias}/query-templates")
public class PlatformQueryTemplateWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformQueryTemplate, PlatformQueryTemplateService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("moduleAlias", pathVariable(request, "moduleAlias"));
    }

    @Override
    protected void bindScope(PlatformQueryTemplate record, WebRequestScope request) {
        record.setModuleAlias(pathVariable(request, "moduleAlias"));
    }

    @Override
    protected boolean inScope(PlatformQueryTemplate record, WebRequestScope request) {
        return Objects.equals(record.getModuleAlias(), pathVariable(request, "moduleAlias"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "Query template does not belong to module: " + pathVariable(request, "moduleAlias") + "." + id;
    }
}
