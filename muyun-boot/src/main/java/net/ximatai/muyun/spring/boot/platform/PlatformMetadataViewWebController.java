package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = "platform", alias = MetadataViewService.MODULE_ALIAS, title = "平台元数据视图")
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/views")
public class PlatformMetadataViewWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataView, MetadataViewService> {
    private final ModuleMetadataRelationService relationService;

    public PlatformMetadataViewWebController(ModuleMetadataRelationService relationService) {
        this.relationService = relationService;
    }
    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireRelation(request);
        criteria.eq("relationId", relationId(request));
    }

    @Override
    protected void bindScope(MetadataView record, HttpServletRequest request) {
        requireRelation(request);
        record.setRelationId(relationId(request));
    }

    @Override
    protected boolean inScope(MetadataView record, HttpServletRequest request) {
        requireRelation(request);
        return Objects.equals(record.getRelationId(), relationId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "metadata view does not belong to relation: " + relationId(request) + "." + id;
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
