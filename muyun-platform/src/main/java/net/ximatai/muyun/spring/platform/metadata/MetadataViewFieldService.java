package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MetadataViewFieldService extends AbstractAbilityService<MetadataViewField> implements
        SoftDeleteAbility<MetadataViewField>,
        EnableAbility<MetadataViewField>,
        SortAbility<MetadataViewField> {
    public static final String MODULE_ALIAS = "platform.metadataViewField";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final MetadataViewService viewService;
    private final MetadataFieldService fieldService;
    private final ModuleMetadataRelationService relationService;

    public MetadataViewFieldService(BaseDao<MetadataViewField, String> viewFieldDao,
                                    MetadataViewService viewService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService) {
        super(MODULE_ALIAS, MetadataViewField.class, viewFieldDao);
        this.viewService = viewService;
        this.fieldService = fieldService;
        this.relationService = relationService;
    }

    @Override
    public void beforeInsert(MetadataViewField viewField) {
        normalizeAndValidate(viewField);
    }

    @Override
    public void beforeUpdate(MetadataViewField viewField) {
        normalizeAndValidate(viewField);
    }

    @Override
    public Criteria sortScope(MetadataViewField viewField) {
        return Criteria.of().eq("viewId", viewField.getViewId());
    }

    @Override
    public void validateSortScope(MetadataViewField left, MetadataViewField right) {
        if (!Objects.equals(left.getViewId(), right.getViewId())) {
            throw new PlatformException("Metadata view field sort can only move records within the same view");
        }
    }

    public List<MetadataViewField> listByViewId(String viewId) {
        if (viewId == null || viewId.isBlank()) {
            return List.of();
        }
        return list(Criteria.of()
                .eq("viewId", viewId)
                .eq("enabled", Boolean.TRUE), ALL, Sort.asc("sortOrder"));
    }

    public EntityViewFieldDefinition compile(MetadataViewField viewField) {
        MetadataField field = requireField(viewField.getMetadataFieldId());
        String title = viewField.getTitle() == null || viewField.getTitle().isBlank()
                ? field.getTitle()
                : viewField.getTitle();
        return new EntityViewFieldDefinition(
                field.getFieldName(),
                title,
                !Boolean.FALSE.equals(viewField.getVisible()),
                viewField.getControlType(),
                viewField.getReadOnly(),
                viewField.getRequiredOverride()
        );
    }

    private void normalizeAndValidate(MetadataViewField viewField) {
        MetadataView view = requireView(viewField.getViewId());
        MetadataField field = requireField(viewField.getMetadataFieldId());
        ModuleMetadataRelation relation = relationService.select(view.getRelationId());
        if (relation == null) {
            throw new PlatformException("Metadata view requires existing relation: " + view.getRelationId());
        }
        if (!relation.getMetadataId().equals(field.getMetadataId())) {
            throw new PlatformException("View field metadata mismatch: " + viewField.getMetadataFieldId());
        }
        if (viewField.getTitle() == null || viewField.getTitle().isBlank()) {
            viewField.setTitle(field.getTitle());
        }
        if (viewField.getVisible() == null) {
            viewField.setVisible(Boolean.TRUE);
        }
        if (viewField.getReadOnly() == null) {
            viewField.setReadOnly(Boolean.FALSE);
        }
        if (Boolean.TRUE.equals(field.getRequired()) && Boolean.FALSE.equals(viewField.getRequiredOverride())) {
            throw new PlatformException("View field cannot make required metadata field optional: " + field.getFieldName());
        }
        rejectDuplicate(viewField, Criteria.of()
                        .eq("viewId", viewField.getViewId())
                        .eq("metadataFieldId", viewField.getMetadataFieldId()),
                "metadata view field must be unique in view: " + viewField.getMetadataFieldId());
    }

    private MetadataView requireView(String viewId) {
        MetadataView view = viewId == null || viewId.isBlank() ? null : viewService.select(viewId);
        if (view == null) {
            throw new PlatformException("Metadata view field requires existing view: " + viewId);
        }
        return view;
    }

    private MetadataField requireField(String metadataFieldId) {
        MetadataField field = metadataFieldId == null || metadataFieldId.isBlank() ? null : fieldService.select(metadataFieldId);
        if (field == null) {
            throw new PlatformException("Metadata view field requires existing metadata field: " + metadataFieldId);
        }
        return field;
    }
}
