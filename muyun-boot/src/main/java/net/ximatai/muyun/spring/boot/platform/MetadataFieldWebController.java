package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MetadataFieldService.MODULE_ALIAS, title = "平台元数据字段")
@Path("/platform.metadata/{metadataId}/fields")
public class MetadataFieldWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataField, MetadataFieldService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("metadataId", metadataId(request));
    }

    @Override
    protected void bindScope(MetadataField record, WebRequestScope request) {
        record.setMetadataId(metadataId(request));
    }

    @Override
    protected boolean inScope(MetadataField record, WebRequestScope request) {
        return metadataId(request).equals(record.getMetadataId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "metadata field does not belong to metadata: " + metadataId(request) + "." + id;
    }

    private String metadataId(WebRequestScope request) {
        return pathVariable(request, "metadataId");
    }
}
