package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MetadataFieldReferenceConfigService.MODULE_ALIAS,
        title = "平台字段引用配置")
@RequestMapping("/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs")
public class MetadataFieldReferenceConfigWebController
        extends NestedCrudWebSupport<MetadataFieldReferenceConfig, MetadataFieldReferenceConfigService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "metadataFieldId", "relationId", "targetModuleAlias", "targetMetadataId",
            "cardinality", "autoTitle", "titleOutputField", "projectionMappings",
            "createdAt", "updatedAt");

    private final MetadataFieldService fieldService;

    public MetadataFieldReferenceConfigWebController(MetadataFieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireField(request);
        criteria.eq("metadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(MetadataFieldReferenceConfig record, HttpServletRequest request) {
        requireField(request);
        record.setMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(MetadataFieldReferenceConfig record, HttpServletRequest request) {
        requireField(request);
        return Objects.equals(record.getMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "metadata field reference config does not belong to field: " + fieldId(request) + "." + id;
    }

    private MetadataField requireField(HttpServletRequest request) {
        MetadataField field = fieldService.select(fieldId(request));
        if (field == null || !Objects.equals(field.getMetadataId(), metadataId(request))) {
            throw new IllegalArgumentException("metadata field does not belong to metadata: "
                    + metadataId(request) + "." + fieldId(request));
        }
        return field;
    }

    private String metadataId(HttpServletRequest request) {
        return pathVariable(request, "metadataId");
    }

    private String fieldId(HttpServletRequest request) {
        return pathVariable(request, "fieldId");
    }
}
