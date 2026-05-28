package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
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

@Service
public class PlatformModuleDefinitionCompiler {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final ModuleMetadataRelationService relationService;
    private final ModuleDefinitionValidator validator;

    public PlatformModuleDefinitionCompiler(PlatformModuleService moduleService,
                                            MetadataService metadataService,
                                            MetadataFieldService fieldService,
                                            ModuleMetadataRelationService relationService) {
        this(moduleService, metadataService, fieldService, relationService, new ModuleDefinitionValidator());
    }

    public PlatformModuleDefinitionCompiler(PlatformModuleService moduleService,
                                            MetadataService metadataService,
                                            MetadataFieldService fieldService,
                                            ModuleMetadataRelationService relationService,
                                            ModuleDefinitionValidator validator) {
        this.moduleService = moduleService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
        this.relationService = relationService;
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
        ModuleDefinition definition = new ModuleDefinition(module.getAlias(), module.getTitle(), entities, childRelations);
        validator.validate(definition);
        if (!mainRelation.getMetadataId().equals(relations.getFirst().getMetadataId())) {
            return orderMainEntityFirst(definition, mainRelation, metadataById);
        }
        return definition;
    }

    private PlatformModule requireDynamicModule(String moduleAlias) {
        PlatformModule module = moduleService.select(moduleAlias);
        if (module == null) {
            throw new AbilityException("Dynamic publish requires existing module: " + moduleAlias);
        }
        if (module.getModuleKind() != ModuleKind.DYNAMIC) {
            throw new AbilityException("Dynamic publish requires DYNAMIC module: " + moduleAlias);
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
                .orElseThrow(() -> new AbilityException("Dynamic module requires MAIN metadata relation: " + moduleAlias));
    }

    private Map<String, Metadata> metadataById(List<ModuleMetadataRelation> relations) {
        Map<String, Metadata> values = new LinkedHashMap<>();
        for (ModuleMetadataRelation relation : relations) {
            Metadata metadata = metadataService.select(relation.getMetadataId());
            if (metadata == null) {
                throw new AbilityException("Module relation points to missing metadata: " + relation.getMetadataId());
            }
            values.put(metadata.getId(), metadata);
        }
        return values;
    }

    private EntityDefinition entity(ModuleMetadataRelation relation,
                                    Metadata metadata,
                                    List<ModuleMetadataRelation> relations) {
        List<FieldDefinition> fields = fields(metadata.getId());
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

    private List<FieldDefinition> fields(String metadataId) {
        return fieldService.list(
                        Criteria.of().eq("metadataId", metadataId),
                        ALL,
                        Sort.asc(PlatformAbilityFields.SORT_FIELD)
                )
                .stream()
                .map(MetadataField::toDefinition)
                .toList();
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
            throw new AbilityException("Child relation metadata is incomplete: " + relation.getRelationAlias());
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

    private ModuleDefinition orderMainEntityFirst(ModuleDefinition definition,
                                                 ModuleMetadataRelation mainRelation,
                                                 Map<String, Metadata> metadataById) {
        String mainEntityCode = metadataById.get(mainRelation.getMetadataId()).getAlias();
        List<EntityDefinition> ordered = definition.entities().stream()
                .sorted((left, right) -> Boolean.compare(!left.code().equals(mainEntityCode), !right.code().equals(mainEntityCode)))
                .toList();
        return new ModuleDefinition(definition.moduleAlias(), definition.name(), ordered, definition.relations(), definition.references());
    }
}
