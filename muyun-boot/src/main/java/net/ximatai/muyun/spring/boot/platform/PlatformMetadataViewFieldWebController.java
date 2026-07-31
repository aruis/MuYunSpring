package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewField;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = MetadataViewFieldService.MODULE_ALIAS,
        title = "平台元数据视图字段", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields")
public class PlatformMetadataViewFieldWebController
        extends NestedEnabledSortableCrudWebSupport<MetadataViewField, MetadataViewFieldService> {

    private final ModuleMetadataRelationService relationService;
    private final MetadataViewService viewService;

    public PlatformMetadataViewFieldWebController(ModuleMetadataRelationService relationService,
                                                  MetadataViewService viewService) {
        this.relationService = relationService;
        this.viewService = viewService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireView(request);
        criteria.eq("viewId", viewId(request));
    }

    @Override
    protected void bindScope(MetadataViewField record, HttpServletRequest request) {
        requireView(request);
        record.setViewId(viewId(request));
    }

    @Override
    protected boolean inScope(MetadataViewField record, HttpServletRequest request) {
        requireView(request);
        return Objects.equals(record.getViewId(), viewId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "metadata view field does not belong to view: " + viewId(request) + "." + id;
    }

    private MetadataView requireView(HttpServletRequest request) {
        requireRelation(request);
        MetadataView view = viewService.select(viewId(request));
        if (view == null || !Objects.equals(view.getRelationId(), relationId(request))) {
            throw new IllegalArgumentException("metadata view does not belong to relation: "
                    + relationId(request) + "." + viewId(request));
        }
        return view;
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

    private String viewId(HttpServletRequest request) {
        return pathVariable(request, "viewId");
    }
}
