package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
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
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        requireField(request);
        criteria.eq("metadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(MetadataFieldProtectionConfig record, @Context HttpServletRequest request) {
        requireField(request);
        record.setMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(MetadataFieldProtectionConfig record, @Context HttpServletRequest request) {
        requireField(request);
        return Objects.equals(record.getMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "metadata field protection config does not belong to field: " + fieldId(request) + "." + id;
    }

    private MetadataField requireField(@Context HttpServletRequest request) {
        MetadataField field = fieldService.select(fieldId(request));
        if (field == null || !Objects.equals(field.getMetadataId(), metadataId(request))) {
            throw new IllegalArgumentException("metadata field does not belong to metadata: "
                    + metadataId(request) + "." + fieldId(request));
        }
        return field;
    }

    private String metadataId(@Context HttpServletRequest request) {
        return pathVariable(request, "metadataId");
    }

    private String fieldId(@Context HttpServletRequest request) {
        return pathVariable(request, "fieldId");
    }
}
