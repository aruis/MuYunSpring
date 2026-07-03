package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItem;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformQueryItemService.MODULE_ALIAS, title = "平台查询项")
@Path("/platform.query-template/{queryTemplateId}/items")
public class PlatformQueryItemWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformQueryItem, PlatformQueryItemService> {

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("queryTemplateId", pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected void bindScope(PlatformQueryItem record, @Context HttpServletRequest request) {
        record.setQueryTemplateId(pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected boolean inScope(PlatformQueryItem record, @Context HttpServletRequest request) {
        return Objects.equals(record.getQueryTemplateId(), pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "Query item does not belong to query template: "
                + pathVariable(request, "queryTemplateId") + "." + id;
    }
}
