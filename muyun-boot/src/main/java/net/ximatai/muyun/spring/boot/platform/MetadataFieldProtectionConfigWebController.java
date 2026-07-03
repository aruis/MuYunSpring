package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldProtectionConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldProtectionConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MetadataFieldProtectionConfigService.MODULE_ALIAS,
        title = "平台字段保护配置")
@Path("/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs")
public class MetadataFieldProtectionConfigWebController
        extends NestedCrudWebSupport<MetadataFieldProtectionConfig, MetadataFieldProtectionConfigService> {

    private final MetadataFieldService fieldService;

    public MetadataFieldProtectionConfigWebController(MetadataFieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        requireField(request);
        criteria.eq("metadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(MetadataFieldProtectionConfig record, WebRequestScope request) {
        requireField(request);
        record.setMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(MetadataFieldProtectionConfig record, WebRequestScope request) {
        requireField(request);
        return Objects.equals(record.getMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "metadata field protection config does not belong to field: " + fieldId(request) + "." + id;
    }

    private MetadataField requireField(WebRequestScope request) {
        MetadataField field = fieldService.select(fieldId(request));
        if (field == null || !Objects.equals(field.getMetadataId(), metadataId(request))) {
            throw new IllegalArgumentException("metadata field does not belong to metadata: "
                    + metadataId(request) + "." + fieldId(request));
        }
        return field;
    }

    private String metadataId(WebRequestScope request) {
        return pathVariable(request, "metadataId");
    }

    private String fieldId(WebRequestScope request) {
        return pathVariable(request, "fieldId");
    }
}
