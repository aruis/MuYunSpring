package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffect;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffectService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = ModuleMetadataFieldAffectService.MODULE_ALIAS,
        title = "平台模块字段引用回填")
@Path("/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects")
public class PlatformModuleMetadataFieldAffectWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataFieldAffect, ModuleMetadataFieldAffectService> {

    private final ModuleMetadataRelationService relationService;
    private final ModuleMetadataFieldService fieldService;

    public PlatformModuleMetadataFieldAffectWebController(ModuleMetadataRelationService relationService,
                                                          ModuleMetadataFieldService fieldService) {
        this.relationService = relationService;
        this.fieldService = fieldService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        requireField(request);
        criteria.eq("moduleMetadataFieldId", fieldId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataFieldAffect record, WebRequestScope request) {
        requireField(request);
        record.setModuleMetadataFieldId(fieldId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataFieldAffect record, WebRequestScope request) {
        requireField(request);
        return Objects.equals(record.getModuleMetadataFieldId(), fieldId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "module metadata field affect does not belong to field: " + fieldId(request) + "." + id;
    }

    private ModuleMetadataField requireField(WebRequestScope request) {
        requireRelation(request);
        ModuleMetadataField field = fieldService.select(fieldId(request));
        if (field == null || !Objects.equals(field.getRelationId(), relationId(request))) {
            throw new IllegalArgumentException("module metadata field does not belong to relation: "
                    + relationId(request) + "." + fieldId(request));
        }
        return field;
    }

    private ModuleMetadataRelation requireRelation(WebRequestScope request) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
        ModuleMetadataRelation relation = relationService.select(relationId(request));
        if (relation == null || !validModuleAlias.equals(relation.getModuleAlias())) {
            throw new IllegalArgumentException("metadata relation does not belong to module: "
                    + validModuleAlias + "." + relationId(request));
        }
        return relation;
    }

    private String relationId(WebRequestScope request) {
        return pathVariable(request, "relationId");
    }

    private String fieldId(WebRequestScope request) {
        return pathVariable(request, "fieldId");
    }
}
