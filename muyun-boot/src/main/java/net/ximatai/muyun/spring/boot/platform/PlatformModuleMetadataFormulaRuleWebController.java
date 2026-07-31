package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFormulaRule;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFormulaRuleService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = ModuleMetadataFormulaRuleService.MODULE_ALIAS,
        title = "平台模块公式规则", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules")
public class PlatformModuleMetadataFormulaRuleWebController
        extends NestedEnabledSortableCrudWebSupport<ModuleMetadataFormulaRule, ModuleMetadataFormulaRuleService> {

    private final ModuleMetadataRelationService relationService;

    public PlatformModuleMetadataFormulaRuleWebController(ModuleMetadataRelationService relationService) {
        this.relationService = relationService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireRelation(request);
        criteria.eq("relationId", relationId(request));
    }

    @Override
    protected void bindScope(ModuleMetadataFormulaRule record, HttpServletRequest request) {
        requireRelation(request);
        record.setRelationId(relationId(request));
    }

    @Override
    protected boolean inScope(ModuleMetadataFormulaRule record, HttpServletRequest request) {
        requireRelation(request);
        return Objects.equals(record.getRelationId(), relationId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "module metadata formula rule does not belong to relation: " + relationId(request) + "." + id;
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
