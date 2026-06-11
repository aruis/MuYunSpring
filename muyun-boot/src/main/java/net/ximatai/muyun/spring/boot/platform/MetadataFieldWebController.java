package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MetadataFieldService.MODULE_ALIAS, title = "平台元数据字段")
@RequestMapping("/platform.metadata/{metadataId}/fields")
public class MetadataFieldWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataField, MetadataFieldService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "metadataId", "fieldName", "columnName", "fieldTypeAlias",
            "fieldOwnership", "fieldForm", "ownerFieldId", "fieldRole", "systemManaged",
            "required", "uniqueField", "indexed", "sortableField", "titleField",
            "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }

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
