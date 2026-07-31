package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = ModuleMetadataFieldService.MODULE_ALIAS, title = "平台模块字段配置",
        webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields")
public class PlatformModuleMetadataFieldWebController
        extends NestedSortableCrudWebSupport<ModuleMetadataField, ModuleMetadataFieldService> {

    private final ModuleMetadataRelationService relationService;

    public PlatformModuleMetadataFieldWebController(ModuleMetadataRelationService relationService) {
        this.relationService = relationService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireRelation(request);
        criteria.eq("relationId", relationId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataField record, HttpServletRequest request) {
        requireRelation(request);
        record.setRelationId(relationId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataField record, HttpServletRequest request) {
        requireRelation(request);
        return relationId(request).equals(record.getRelationId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "module metadata field does not belong to relation: " + relationId(request) + "." + id;
    }

    @PostMapping("/ensure")
    @CustomActionEndpoint(value = "ensureFields", title = "同步模块字段配置",
            level = PlatformActionLevel.LIST, dataAuth = false)
    public WebListResponse<ModuleMetadataField> ensure(HttpServletRequest request) {
        return webScope(() -> {
            requireRelation(request);
            return new WebListResponse<>(WebOutputSupport.records(service(),
                    service().ensureForRelation(relationId(request)), FieldOutputContext.VIEW));
        });
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
}
