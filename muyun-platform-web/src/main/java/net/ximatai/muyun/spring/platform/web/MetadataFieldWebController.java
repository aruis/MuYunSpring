package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = MetadataFieldService.MODULE_ALIAS, title = "平台元数据字段",
        webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.metadata/{metadataId}/fields")
public class MetadataFieldWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataField, MetadataFieldService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("metadataId", metadataId(request));
    }

    @Override
    protected void bindScope(MetadataField record, HttpServletRequest request) {
        record.setMetadataId(metadataId(request));
    }

    @Override
    protected boolean inScope(MetadataField record, HttpServletRequest request) {
        return metadataId(request).equals(record.getMetadataId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "metadata field does not belong to metadata: " + metadataId(request) + "." + id;
    }

    private String metadataId(HttpServletRequest request) {
        return pathVariable(request, "metadataId");
    }
}
