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
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;

@Service
public class MetadataViewFieldService extends AbstractAbilityService<MetadataViewField> implements
        SoftDeleteAbility<MetadataViewField>,
        EnableAbility<MetadataViewField>,
        SortAbility<MetadataViewField>,
        QueryAbility<MetadataViewField> {
    public static final String MODULE_ALIAS = "platform.metadata_view_field";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final MetadataViewService viewService;
    private final MetadataFieldService fieldService;
    private final ModuleMetadataRelationService relationService;
    private final FieldUiControlService fieldUiTypeService;
    private final FieldSpecService fieldTypeService;
    private final PlatformDynamicRuntimeRefreshCoordinator runtimeRefreshCoordinator;

    public MetadataViewFieldService(BaseDao<MetadataViewField, String> viewFieldDao,
                                    MetadataViewService viewService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService) {
        this(viewFieldDao, viewService, fieldService, relationService, null, null);
    }

    public MetadataViewFieldService(BaseDao<MetadataViewField, String> viewFieldDao,
                                    MetadataViewService viewService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService,
                                    FieldUiControlService fieldUiTypeService,
                                    FieldSpecService fieldTypeService) {
        this(viewFieldDao, viewService, fieldService, relationService, fieldUiTypeService, fieldTypeService,
                Optional.empty());
    }

    @Autowired
    public MetadataViewFieldService(BaseDao<MetadataViewField, String> viewFieldDao,
                                    MetadataViewService viewService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService,
                                    FieldUiControlService fieldUiTypeService,
                                    FieldSpecService fieldTypeService,
                                    Optional<PlatformDynamicRuntimeRefreshCoordinator> runtimeRefreshCoordinator) {
        super(MODULE_ALIAS, MetadataViewField.class, viewFieldDao);
        this.viewService = viewService;
        this.fieldService = fieldService;
        this.relationService = relationService;
        this.fieldUiTypeService = fieldUiTypeService;
        this.fieldTypeService = fieldTypeService;
        this.runtimeRefreshCoordinator = runtimeRefreshCoordinator.orElse(null);
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptors.fromModel(MODULE_ALIAS, MetadataViewField.class, java.util.List.of("id", "viewId", "metadataFieldId", "visible", "controlType", "fieldUiControlAlias", "readOnly", "requiredOverride", "title", "enabled", "sortOrder", "createdAt", "updatedAt"),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
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
    public void afterChanged(MetadataViewField viewField) {
        if (runtimeRefreshCoordinator != null) {
            runtimeRefreshCoordinator.refreshByMetadataViewField(viewField);
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
                viewField.getFieldUiControlAlias(),
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
        if (viewField.getFieldUiControlAlias() != null && !viewField.getFieldUiControlAlias().isBlank()) {
            viewField.setFieldUiControlAlias(PlatformNameRules.requireIdentifier(
                    viewField.getFieldUiControlAlias(), "fieldUiControlAlias"));
            if (fieldUiTypeService != null) {
                FieldUiControl fieldUiType = fieldUiTypeService.requireFieldUiControl(viewField.getFieldUiControlAlias());
                validateUiTypeCompatibility(field, fieldUiType);
                if (viewField.getControlType() == null) {
                    viewField.setControlType(fieldUiType.getRendererType());
                }
            }
        }
        if (Boolean.TRUE.equals(field.getRequired()) && Boolean.FALSE.equals(viewField.getRequiredOverride())) {
            throw new PlatformException("View field cannot make required metadata field optional: " + field.getFieldName());
        }
        rejectDuplicate(viewField, Criteria.of()
                        .eq("viewId", viewField.getViewId())
                        .eq("metadataFieldId", viewField.getMetadataFieldId()),
                "metadata view field must be unique in view: " + viewField.getMetadataFieldId());
    }

    private void validateUiTypeCompatibility(MetadataField field, FieldUiControl fieldUiType) {
        if (fieldTypeService == null) {
            return;
        }
        FieldSpec fieldType = fieldTypeService.requireFieldType(field.getFieldSpecAlias());
        if (fieldType.getUiControlAliases() != null && !fieldType.getUiControlAliases().isEmpty()) {
            if (!fieldType.getUiControlAliases().contains(fieldUiType.getAlias())) {
                throw new PlatformException("Field UI type is not allowed by field type: "
                        + field.getFieldName() + "." + fieldUiType.getAlias());
            }
            return;
        }
        if (fieldUiType.getDefaultFieldSpecAlias() != null
                && !fieldUiType.getDefaultFieldSpecAlias().equals(fieldType.getAlias())) {
            throw new PlatformException("Field UI type default field type mismatch: "
                    + field.getFieldName() + "." + fieldUiType.getAlias());
        }
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
