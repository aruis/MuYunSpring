package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttribute;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttributeService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformFieldUiTypeAttributeService.MODULE_ALIAS,
        title = "平台字段 UI 类型属性")
@Path("/platform.field_ui_type/{fieldUiTypeAlias}/attributes")
public class PlatformFieldUiTypeAttributeWebController
        extends NestedSortableCrudWebSupport<PlatformFieldUiTypeAttribute, PlatformFieldUiTypeAttributeService> {

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("fieldUiTypeAlias", fieldUiTypeAlias(request));
    }

    @Override
    protected void bindScope(PlatformFieldUiTypeAttribute record, @Context HttpServletRequest request) {
        record.setFieldUiTypeAlias(fieldUiTypeAlias(request));
    }

    @Override
    protected boolean inScope(PlatformFieldUiTypeAttribute record, @Context HttpServletRequest request) {
        return Objects.equals(record.getFieldUiTypeAlias(), fieldUiTypeAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "field UI type attribute does not belong to field UI type: "
                + fieldUiTypeAlias(request) + "." + id;
    }

    private String fieldUiTypeAlias(@Context HttpServletRequest request) {
        return PlatformNameRules.requireIdentifier(pathVariable(request, "fieldUiTypeAlias"), "fieldUiTypeAlias");
    }
}
