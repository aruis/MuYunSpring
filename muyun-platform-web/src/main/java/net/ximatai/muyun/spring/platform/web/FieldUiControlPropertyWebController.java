package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.FieldUiControlProperty;
import net.ximatai.muyun.spring.platform.metadata.FieldUiControlPropertyService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticWebScope(PlatformStaticWebScope.Scope.CUSTOM)
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = FieldUiControlPropertyService.MODULE_ALIAS,
        title = "平台字段 UI 类型属性")
@RequestMapping("/platform.field_ui_control/{fieldUiControlAlias}/properties")
public class FieldUiControlPropertyWebController
        extends NestedSortableCrudWebSupport<FieldUiControlProperty, FieldUiControlPropertyService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("fieldUiControlAlias", fieldUiControlAlias(request));
    }

    @Override
    protected void bindScope(FieldUiControlProperty record, HttpServletRequest request) {
        record.setFieldUiControlAlias(fieldUiControlAlias(request));
    }

    @Override
    protected boolean inScope(FieldUiControlProperty record, HttpServletRequest request) {
        return Objects.equals(record.getFieldUiControlAlias(), fieldUiControlAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "field UI control property does not belong to field UI control: "
                + fieldUiControlAlias(request) + "." + id;
    }

    private String fieldUiControlAlias(HttpServletRequest request) {
        return PlatformNameRules.requireIdentifier(pathVariable(request, "fieldUiControlAlias"), "fieldUiControlAlias");
    }
}
