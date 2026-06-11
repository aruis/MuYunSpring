package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffect;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffectService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = ModuleMetadataFieldAffectService.MODULE_ALIAS,
        title = "平台模块字段引用回填")
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects")
public class PlatformModuleMetadataFieldAffectWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataFieldAffect, ModuleMetadataFieldAffectService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "moduleMetadataFieldId", "referenceFieldId", "targetFieldId",
            "sortOrder", "createdAt", "updatedAt");

    private final ModuleMetadataRelationService relationService;
    private final ModuleMetadataFieldService fieldService;

    public PlatformModuleMetadataFieldAffectWebController(ModuleMetadataRelationService relationService,
                                                          ModuleMetadataFieldService fieldService) {
        this.relationService = relationService;
        this.fieldService = fieldService;
    }

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
        requireField(request);
        criteria.eq("moduleMetadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataFieldAffect record, HttpServletRequest request) {
        requireField(request);
        record.setModuleMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataFieldAffect record, HttpServletRequest request) {
        requireField(request);
        return Objects.equals(record.getModuleMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "module metadata field affect does not belong to field: " + fieldId(request) + "." + id;
    }

    private ModuleMetadataField requireField(HttpServletRequest request) {
        requireRelation(request);
        ModuleMetadataField field = fieldService.select(fieldId(request));
        if (field == null || !Objects.equals(field.getRelationId(), relationId(request))) {
            throw new IllegalArgumentException("module metadata field does not belong to relation: "
                    + relationId(request) + "." + fieldId(request));
        }
        return field;
    }

    private ModuleMetadataRelation requireRelation(HttpServletRequest request) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
        ModuleMetadataRelation relation = relationService.select(relationId(request));
        if (relation == null || !validModuleAlias.equals(relation.getModuleAlias())) {
            throw new IllegalArgumentException("metadata relation does not belong to module: "
                    + validModuleAlias + "." + relationId(request));
        }
        return relation;
    }

    private String relationId(HttpServletRequest request) {
        return pathVariable(request, "relationId");
    }

    private String fieldId(HttpServletRequest request) {
        return pathVariable(request, "fieldId");
    }
}
