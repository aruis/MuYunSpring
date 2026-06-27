package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttribute;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttributeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformFieldUiTypeAttributeService.MODULE_ALIAS,
        title = "平台字段 UI 类型属性")
@RequestMapping("/platform.field_ui_type/{fieldUiTypeAlias}/attributes")
public class PlatformFieldUiTypeAttributeWebController
        extends NestedSortableCrudWebSupport<PlatformFieldUiTypeAttribute, PlatformFieldUiTypeAttributeService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("fieldUiTypeAlias", fieldUiTypeAlias(request));
    }

    @Override
    protected void bindScope(PlatformFieldUiTypeAttribute record, HttpServletRequest request) {
        record.setFieldUiTypeAlias(fieldUiTypeAlias(request));
    }

    @Override
    protected boolean inScope(PlatformFieldUiTypeAttribute record, HttpServletRequest request) {
        return Objects.equals(record.getFieldUiTypeAlias(), fieldUiTypeAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "field UI type attribute does not belong to field UI type: "
                + fieldUiTypeAlias(request) + "." + id;
    }

    private String fieldUiTypeAlias(HttpServletRequest request) {
        return PlatformNameRules.requireIdentifier(pathVariable(request, "fieldUiTypeAlias"), "fieldUiTypeAlias");
    }
}
