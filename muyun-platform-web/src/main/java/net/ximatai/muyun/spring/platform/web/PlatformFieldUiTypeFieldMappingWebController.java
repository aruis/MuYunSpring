package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMapping;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMappingService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = PlatformFieldUiTypeFieldMappingService.MODULE_ALIAS,
        title = "平台字段 UI 类型字段映射", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings")
public class PlatformFieldUiTypeFieldMappingWebController
        extends NestedSortableCrudWebSupport<PlatformFieldUiTypeFieldMapping, PlatformFieldUiTypeFieldMappingService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("fieldUiTypeAlias", fieldUiTypeAlias(request));
    }

    @Override
    protected void bindScope(PlatformFieldUiTypeFieldMapping record, HttpServletRequest request) {
        record.setFieldUiTypeAlias(fieldUiTypeAlias(request));
    }

    @Override
    protected boolean inScope(PlatformFieldUiTypeFieldMapping record, HttpServletRequest request) {
        return Objects.equals(record.getFieldUiTypeAlias(), fieldUiTypeAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "field UI type field mapping does not belong to field UI type: "
                + fieldUiTypeAlias(request) + "." + id;
    }

    private String fieldUiTypeAlias(HttpServletRequest request) {
        return PlatformNameRules.requireIdentifier(pathVariable(request, "fieldUiTypeAlias"), "fieldUiTypeAlias");
    }
}
