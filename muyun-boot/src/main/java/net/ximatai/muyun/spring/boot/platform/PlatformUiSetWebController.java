package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformUiSetService.MODULE_ALIAS, title = "平台 UI 配置集")
@Path("/platform.module/{moduleAlias}/ui-sets")
public class PlatformUiSetWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiSet, PlatformUiSetService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("moduleAlias", pathVariable(request, "moduleAlias"));
    }

    @Override
    protected void bindScope(PlatformUiSet record, WebRequestScope request) {
        record.setModuleAlias(pathVariable(request, "moduleAlias"));
    }

    @Override
    protected boolean inScope(PlatformUiSet record, WebRequestScope request) {
        return Objects.equals(record.getModuleAlias(), pathVariable(request, "moduleAlias"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "UI set does not belong to module: " + pathVariable(request, "moduleAlias") + "." + id;
    }
}
