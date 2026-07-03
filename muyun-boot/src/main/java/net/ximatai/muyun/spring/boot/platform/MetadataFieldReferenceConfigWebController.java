package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MetadataFieldReferenceConfigService.MODULE_ALIAS,
        title = "平台字段引用配置")
@Path("/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs")
public class MetadataFieldReferenceConfigWebController
        extends NestedCrudWebSupport<MetadataFieldReferenceConfig, MetadataFieldReferenceConfigService> {

    private final MetadataFieldService fieldService;

    public MetadataFieldReferenceConfigWebController(MetadataFieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        requireField(request);
        criteria.eq("metadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(MetadataFieldReferenceConfig record, @Context HttpServletRequest request) {
        requireField(request);
        record.setMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(MetadataFieldReferenceConfig record, @Context HttpServletRequest request) {
        requireField(request);
        return Objects.equals(record.getMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "metadata field reference config does not belong to field: " + fieldId(request) + "." + id;
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
