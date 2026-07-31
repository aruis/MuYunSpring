package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldFilter;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldFilterService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = ModuleMetadataFieldFilterService.MODULE_ALIAS,
        title = "平台模块字段引用过滤", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters")
public class PlatformModuleMetadataFieldFilterWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataFieldFilter, ModuleMetadataFieldFilterService> {

    private final ModuleMetadataRelationService relationService;
    private final ModuleMetadataFieldService fieldService;

    public PlatformModuleMetadataFieldFilterWebController(ModuleMetadataRelationService relationService,
                                                          ModuleMetadataFieldService fieldService) {
        this.relationService = relationService;
        this.fieldService = fieldService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireField(request);
        criteria.eq("moduleMetadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataFieldFilter record, HttpServletRequest request) {
        requireField(request);
        record.setModuleMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataFieldFilter record, HttpServletRequest request) {
        requireField(request);
        return Objects.equals(record.getModuleMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "module metadata field filter does not belong to field: " + fieldId(request) + "." + id;
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
