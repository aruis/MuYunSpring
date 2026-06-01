package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.platform.metadata.MetadataAction;
import net.ximatai.muyun.spring.platform.metadata.MetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldDefinitionCompiler;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PlatformModuleDefinitionCompiler {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final MetadataFieldDefinitionCompiler fieldDefinitionCompiler;
    private final MetadataFieldReferenceConfigService referenceConfigService;
    private final ModuleMetadataRelationService relationService;
    private final MetadataViewService viewService;
    private final MetadataViewFieldService viewFieldService;
    private final MetadataActionService actionService;
    private final ModuleDefinitionValidator validator;

    public PlatformModuleDefinitionCompiler(PlatformModuleService moduleService,
                                            MetadataService metadataService,
                                            MetadataFieldService fieldService,
                                            MetadataFieldDefinitionCompiler fieldDefinitionCompiler,
                                            MetadataFieldReferenceConfigService referenceConfigService,
                                            ModuleMetadataRelationService relationService,
                                            MetadataViewService viewService,
                                            MetadataViewFieldService viewFieldService,
                                            MetadataActionService actionService) {
        this(moduleService, metadataService, fieldService, fieldDefinitionCompiler, referenceConfigService, relationService,
                viewService, viewFieldService, actionService,
                new ModuleDefinitionValidator());
    }

    public PlatformModuleDefinitionCompiler(PlatformModuleService moduleService,
                                            MetadataService metadataService,
                                            MetadataFieldService fieldService,
                                            MetadataFieldDefinitionCompiler fieldDefinitionCompiler,
                                            MetadataFieldReferenceConfigService referenceConfigService,
                                            ModuleMetadataRelationService relationService,
                                            MetadataViewService viewService,
                                            MetadataViewFieldService viewFieldService,
                                            MetadataActionService actionService,
                                            ModuleDefinitionValidator validator) {
        this.moduleService = moduleService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
        this.fieldDefinitionCompiler = fieldDefinitionCompiler;
        this.referenceConfigService = referenceConfigService;
        this.relationService = relationService;
        this.viewService = viewService;
        this.viewFieldService = viewFieldService;
        this.actionService = actionService;
        this.validator = validator;
    }

    public ModuleDefinition compile(String moduleAlias) {
        PlatformModule module = requireDynamicModule(moduleAlias);
        List<ModuleMetadataRelation> relations = relations(moduleAlias);
        ModuleMetadataRelation mainRelation = requireMainRelation(moduleAlias, relations);
        Map<String, Metadata> metadataById = metadataById(relations);
        List<EntityDefinition> entities = relations.stream()
                .map(relation -> entity(relation, metadataById.get(relation.getMetadataId()), relations))
                .toList();
        List<EntityRelationDefinition> childRelations = relations.stream()
                .filter(relation -> relation.getRelationRole() == RelationRole.CHILD)
                .map(relation -> childRelation(relation, metadataById))
                .toList();
        List<EntityReferenceDefinition> references = references(module.getAlias(), relations, metadataById);
        List<EntityViewDefinition> views = views(relations, metadataById);
        List<EntityActionDefinition> actions = actions(relations, metadataById);
        List<EntityAssociationViewDefinition> associationViews = associationViews(module.getAlias(), childRelations,
                references);
        String mainEntityCode = metadataById.get(mainRelation.getMetadataId()).getAlias();
        ModuleDefinition definition = new ModuleDefinition(module.getAlias(), module.getTitle(), entities, childRelations,
                references, views, associationViews, actions, mainEntityCode);
        validator.validate(definition);
        if (!mainRelation.getMetadataId().equals(relations.getFirst().getMetadataId())) {
            return orderMainEntityFirst(definition, mainRelation, metadataById);
        }
        return definition;
    }

    private PlatformModule requireDynamicModule(String moduleAlias) {
        PlatformModule module = moduleService.select(moduleAlias);
        if (module == null) {
            throw new PlatformException("Dynamic publish requires existing module: " + moduleAlias);
        }
        if (module.getModuleKind() != ModuleKind.DYNAMIC) {
            throw new PlatformException("Dynamic publish requires DYNAMIC module: " + moduleAlias);
        }
        return module;
    }

    private List<ModuleMetadataRelation> relations(String moduleAlias) {
        return relationService.list(
                Criteria.of().eq("moduleAlias", moduleAlias),
                ALL,
                Sort.asc(PlatformAbilityFields.SORT_FIELD)
        );
    }

    private ModuleMetadataRelation requireMainRelation(String moduleAlias, List<ModuleMetadataRelation> relations) {
        return relations.stream()
                .filter(relation -> relation.getRelationRole() == RelationRole.MAIN)
                .findFirst()
                .orElseThrow(() -> new PlatformException("Dynamic module requires MAIN metadata relation: " + moduleAlias));
    }

    private Map<String, Metadata> metadataById(List<ModuleMetadataRelation> relations) {
        Map<String, Metadata> values = new LinkedHashMap<>();
        for (ModuleMetadataRelation relation : relations) {
            Metadata metadata = metadataService.select(relation.getMetadataId());
            if (metadata == null) {
                throw new PlatformException("Module relation points to missing metadata: " + relation.getMetadataId());
            }
            values.put(metadata.getId(), metadata);
        }
        return values;
    }

    private EntityDefinition entity(ModuleMetadataRelation relation,
                                    Metadata metadata,
                                    List<ModuleMetadataRelation> relations) {
        List<FieldDefinition> fields = fields(metadata.getId(), relation.getId());
        EnumSet<EntityCapability> capabilities = capabilities(fields);
        if (relations.stream().anyMatch(child -> metadata.getId().equals(child.getParentMetadataId()))) {
            capabilities.add(EntityCapability.CHILD_RELATION);
        }
        return new EntityDefinition(metadata.getAlias(),
                metadata.getSchemaName(),
                metadata.getTableName(),
                metadata.getTitle(),
                fields,
                capabilities);
    }

    private List<FieldDefinition> fields(String metadataId, String relationId) {
        return metadataFields(metadataId).stream()
                .map(field -> fieldDefinitionCompiler.compile(field, relationId))
                .toList();
    }

    private List<MetadataField> metadataFields(String metadataId) {
        return fieldService.list(
                        Criteria.of().eq("metadataId", metadataId),
                        ALL,
                        Sort.asc(PlatformAbilityFields.SORT_FIELD)
                );
    }

    private EnumSet<EntityCapability> capabilities(List<FieldDefinition> fields) {
        EnumSet<EntityCapability> capabilities = EnumSet.noneOf(EntityCapability.class);
        capabilities.add(EntityCapability.CRUD);
        for (FieldDefinition field : fields) {
            if (PlatformAbilityFields.TREE_PARENT_FIELD.equals(field.fieldName())) {
                capabilities.add(EntityCapability.TREE);
            }
            if (field.isSortable()) {
                capabilities.add(EntityCapability.SORT);
            }
            if (field.isTitle()) {
                capabilities.add(EntityCapability.REFERENCE);
            }
            if (PlatformAbilityFields.ENABLED_FIELD.equals(field.fieldName())
                    || PlatformAbilityFields.ENABLED_COLUMN.equals(field.columnName())) {
                capabilities.add(EntityCapability.ENABLE);
            }
        }
        return capabilities;
    }

    private EntityRelationDefinition childRelation(ModuleMetadataRelation relation, Map<String, Metadata> metadataById) {
        Metadata parent = metadataById.get(relation.getParentMetadataId());
        Metadata child = metadataById.get(relation.getMetadataId());
        if (parent == null || child == null) {
            throw new PlatformException("Child relation metadata is incomplete: " + relation.getRelationAlias());
        }
        EntityRelationDefinition definition = EntityRelationDefinition.child(
                relation.getRelationAlias(),
                parent.getAlias(),
                child.getAlias(),
                relation.getForeignKey()
        );
        if (Boolean.TRUE.equals(relation.getAutoPopulate())) {
            definition = definition.withAutoPopulate();
        }
        if (Boolean.TRUE.equals(relation.getCascadeDelete())) {
            definition = definition.withAutoDeleteWithParent();
        }
        return definition;
    }

    private List<EntityReferenceDefinition> references(String moduleAlias,
                                                       List<ModuleMetadataRelation> relations,
                                                       Map<String, Metadata> metadataById) {
        return relations.stream()
                .flatMap(relation -> references(moduleAlias, relation, metadataById).stream())
                .toList();
    }

    private List<EntityReferenceDefinition> references(String moduleAlias,
                                                       ModuleMetadataRelation relation,
                                                       Map<String, Metadata> metadataById) {
        Metadata sourceMetadata = metadataById.get(relation.getMetadataId());
        return metadataFields(sourceMetadata.getId()).stream()
                .map(field -> reference(moduleAlias, relation, sourceMetadata, field, metadataById))
                .filter(Objects::nonNull)
                .toList();
    }

    private EntityReferenceDefinition reference(String moduleAlias,
                                                ModuleMetadataRelation relation,
                                                Metadata sourceMetadata,
                                                MetadataField sourceField,
                                                Map<String, Metadata> metadataById) {
        MetadataFieldReferenceConfig config = referenceConfigService.findForRelation(sourceField.getId(), relation.getId());
        if (config == null) {
            return null;
        }
        Metadata targetMetadata = metadataById.get(config.getTargetMetadataId());
        if (targetMetadata == null) {
            targetMetadata = metadataService.select(config.getTargetMetadataId());
        }
        if (targetMetadata == null) {
            throw new PlatformException("Reference config points to missing metadata: " + config.getTargetMetadataId());
        }
        String targetModuleAlias = config.getTargetModuleAlias() == null || config.getTargetModuleAlias().isBlank()
                ? moduleAlias
                : config.getTargetModuleAlias();
        EntityReferenceDefinition definition = new EntityReferenceDefinition(
                sourceMetadata.getAlias(),
                sourceField.getFieldName(),
                targetModuleAlias + "." + targetMetadata.getAlias(),
                config.getCardinality(),
                Boolean.TRUE.equals(config.getAutoTitle()),
                config.getTitleOutputField()
        );
        for (ReferenceProjection projection : config.projections()) {
            definition = definition.withProjection(projection.targetField(), projection.outputField());
        }
        return definition;
    }

    private List<EntityViewDefinition> views(List<ModuleMetadataRelation> relations, Map<String, Metadata> metadataById) {
        Map<String, ModuleMetadataRelation> relationById = relations.stream()
                .collect(java.util.stream.Collectors.toMap(ModuleMetadataRelation::getId, relation -> relation));
        return viewService.listByRelationIds(relations.stream().map(ModuleMetadataRelation::getId).toList()).stream()
                .map(view -> view(view, relationById, metadataById))
                .toList();
    }

    private EntityViewDefinition view(MetadataView view,
                                      Map<String, ModuleMetadataRelation> relationById,
                                      Map<String, Metadata> metadataById) {
        ModuleMetadataRelation relation = relationById.get(view.getRelationId());
        if (relation == null) {
            throw new PlatformException("View points to relation outside current module: " + view.getRelationId());
        }
        Metadata metadata = metadataById.get(relation.getMetadataId());
        if (metadata == null) {
            throw new PlatformException("View relation metadata is incomplete: " + view.getRelationId());
        }
        return new EntityViewDefinition(
                metadata.getAlias(),
                view.getViewType(),
                view.getTitle(),
                viewFieldService.listByViewId(view.getId()).stream()
                        .map(viewFieldService::compile)
                        .toList()
        );
    }

    private ModuleDefinition orderMainEntityFirst(ModuleDefinition definition,
                                                 ModuleMetadataRelation mainRelation,
                                                 Map<String, Metadata> metadataById) {
        String mainEntityCode = metadataById.get(mainRelation.getMetadataId()).getAlias();
        List<EntityDefinition> ordered = definition.entities().stream()
                .sorted((left, right) -> Boolean.compare(!left.code().equals(mainEntityCode), !right.code().equals(mainEntityCode)))
                .toList();
        return new ModuleDefinition(definition.moduleAlias(), definition.name(), ordered, definition.relations(),
                definition.references(), definition.views(), definition.associationViews(), definition.actions(),
                definition.mainEntityCode());
    }

    private List<EntityAssociationViewDefinition> associationViews(String moduleAlias,
                                                                   List<EntityRelationDefinition> childRelations,
                                                                   List<EntityReferenceDefinition> references) {
        return java.util.stream.Stream.concat(
                childRelations.stream().map(relation -> EntityAssociationViewDefinition.childRelation(
                        relation.code(),
                        relation.parentEntity(),
                        moduleAlias,
                        relation.childEntity(),
                        relation.code()
                )),
                references.stream().map(reference -> {
                    ReferenceTarget target = reference.target();
                    return EntityAssociationViewDefinition.reference(
                            reference.sourceField(),
                            reference.sourceEntity(),
                            target.moduleAlias(),
                            target.entityCode(),
                            reference.sourceField(),
                            reference.cardinality()
                    );
                })
        ).toList();
    }

    private List<EntityActionDefinition> actions(List<ModuleMetadataRelation> relations, Map<String, Metadata> metadataById) {
        Map<String, ModuleMetadataRelation> relationById = relations.stream()
                .collect(java.util.stream.Collectors.toMap(ModuleMetadataRelation::getId, relation -> relation));
        return actionService.listByRelationIds(relations.stream().map(ModuleMetadataRelation::getId).toList()).stream()
                .map(action -> action(action, relationById, metadataById))
                .toList();
    }

    private EntityActionDefinition action(MetadataAction action,
                                          Map<String, ModuleMetadataRelation> relationById,
                                          Map<String, Metadata> metadataById) {
        ModuleMetadataRelation relation = relationById.get(action.getRelationId());
        if (relation == null) {
            throw new PlatformException("Action points to relation outside current module: " + action.getRelationId());
        }
        Metadata metadata = metadataById.get(relation.getMetadataId());
        if (metadata == null) {
            throw new PlatformException("Action relation metadata is incomplete: " + action.getRelationId());
        }
        return new EntityActionDefinition(
                metadata.getAlias(),
                action.getActionCode(),
                action.getActionKind(),
                action.getTitle(),
                Boolean.TRUE.equals(action.getEnabled()),
                action.getActionLevel(),
                action.getPermissionCode()
        );
    }
}
