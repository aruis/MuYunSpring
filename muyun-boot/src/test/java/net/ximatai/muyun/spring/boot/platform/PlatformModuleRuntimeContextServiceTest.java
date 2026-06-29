package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformModuleRuntimeContextServiceTest {
    @Test
    void shouldComposeStaticCapabilitiesFromActionsAndEntities() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        PlatformModule module = module("iam.organization", "组织管理", ModuleKind.STATIC);
        when(moduleService.resolveVisibleModule("iam.organization")).thenReturn(module);
        when(actionService.listByModuleAliases(List.of("iam.organization"))).thenReturn(List.of());
        StaticModuleDefinition definition = new StaticModuleDefinition(
                "iam",
                "iam.organization",
                "组织管理",
                null,
                ModuleEntryType.ROUTE,
                "/iam/organizations",
                null,
                Set.of(),
                List.of(
                        StaticModuleActionDefinition.platformAction(PlatformAction.MENU),
                        StaticModuleActionDefinition.platformAction(PlatformAction.VIEW),
                        StaticModuleActionDefinition.platformAction(PlatformAction.TREE),
                        StaticModuleActionDefinition.platformAction(PlatformAction.ENABLE),
                        StaticModuleActionDefinition.platformAction(PlatformAction.DISABLE)
                ),
                List.of(),
                ModuleUiDefinition.builder("iam.organization")
                        .listView(list -> list
                                .title("组织列表")
                                .field("title", field -> field.label("组织名称")))
                        .build()
        );
        PlatformModuleRuntimeContextService service = new PlatformModuleRuntimeContextService(
                moduleService,
                actionService,
                new StaticModuleDefinitionCatalog(List.of(definition)),
                null,
                allowAllPolicy()
        );

        PlatformModuleRuntimeContext context = service.context("iam.organization");

        assertThat(context.moduleAlias()).isEqualTo("iam.organization");
        assertThat(context.entryType()).isEqualTo(ModuleEntryType.ROUTE);
        assertThat(context.entryRoute()).isEqualTo("/iam/organizations");
        assertThat(context.capabilities()).contains(
                EntityCapability.CRUD,
                EntityCapability.SOFT_DELETE,
                EntityCapability.LIFECYCLE,
                EntityCapability.CACHE,
                EntityCapability.TREE,
                EntityCapability.SORT,
                EntityCapability.ENABLE
        );
        assertThat(context.abilities()).contains(
                "crud",
                "softDelete",
                "lifecycle",
                "cache",
                "tree",
                "sort",
                "enable"
        );
        assertThat(context.actions()).extracting(PlatformModuleRuntimeAction::actionCode)
                .containsExactly("menu", "view", "tree", "enable", "disable");
        assertThat(context.actions()).allSatisfy(action -> assertThat(action.authorized()).isTrue());
        assertThat(context.uiDefinition()).isNotNull();
        assertThat(context.uiDefinition().views()).singleElement()
                .satisfies(view -> {
                    assertThat(view.viewCode()).isEqualTo("default_list");
                    assertThat(view.fields()).singleElement()
                            .satisfies(field -> assertThat(field.fieldRef().fieldName()).isEqualTo("title"));
                });
        assertThat(context.uiDescriptor()).isNotNull();
        assertThat(context.uiDescriptor().views()).singleElement()
                .satisfies(view -> {
                    assertThat(view.viewCode()).isEqualTo("default_list");
                    assertThat(view.fields()).singleElement()
                            .satisfies(field -> {
                                assertThat(field.fieldRef().fieldName()).isEqualTo("title");
                                assertThat(field.label()).isEqualTo("组织名称");
                            });
                });
    }

    @Test
    void shouldNotExposeSecondaryEntityCapabilitiesAsModuleAbilities() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", "合同", ModuleKind.STATIC));
        when(actionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        StaticModuleDefinition definition = new StaticModuleDefinition(
                "sales",
                "sales.contract",
                "合同",
                null,
                ModuleEntryType.ROUTE,
                "/sales/contracts",
                null,
                Set.of(),
                List.of(StaticModuleActionDefinition.platformAction(PlatformAction.VIEW)),
                List.of(
                        entity("contract", Set.of(EntityCapability.CRUD)),
                        entity("contractLine", Set.of(EntityCapability.CRUD, EntityCapability.TREE,
                                EntityCapability.ENABLE))
                )
        );
        PlatformModuleRuntimeContextService service = new PlatformModuleRuntimeContextService(
                moduleService,
                actionService,
                new StaticModuleDefinitionCatalog(List.of(definition)),
                null,
                allowAllPolicy()
        );

        PlatformModuleRuntimeContext context = service.context("sales.contract");

        assertThat(context.mainEntityAlias()).isEqualTo("contract");
        assertThat(context.abilities()).contains("crud");
        assertThat(context.abilities()).doesNotContain("tree", "enable");
    }

    @Test
    void shouldPreferPersistedModuleActionsAndExposeAuthorizationResult() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("iam.organization"))
                .thenReturn(module("iam.organization", "组织管理", ModuleKind.STATIC));
        PlatformModuleAction view = action("iam.organization", PlatformAction.VIEW);
        PlatformModuleAction enable = action("iam.organization", PlatformAction.ENABLE);
        when(actionService.listByModuleAliases(List.of("iam.organization"))).thenReturn(List.of(view, enable));
        ActionExecutionPolicyService policyService = context -> {
            if (PlatformAction.ENABLE.matches(context.actionCode())) {
                throw new PlatformAccessDeniedException("denied");
            }
        };
        StaticModuleDefinition definition = new StaticModuleDefinition(
                "iam",
                "iam.organization",
                "组织管理",
                null,
                List.of(StaticModuleActionDefinition.platformAction(PlatformAction.TREE))
        );
        PlatformModuleRuntimeContextService service = new PlatformModuleRuntimeContextService(
                moduleService,
                actionService,
                new StaticModuleDefinitionCatalog(List.of(definition)),
                null,
                policyService
        );

        PlatformModuleRuntimeContext context = service.context("iam.organization");

        assertThat(context.actions()).extracting(PlatformModuleRuntimeAction::actionCode)
                .containsExactly("view", "enable");
        assertThat(context.actions()).filteredOn(action -> "view".equals(action.actionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.authorized()).isTrue();
                    assertThat(action.authorizationDecision()).isEqualTo(ActionAuthorizationResult.DECISION_ALLOWED);
                });
        assertThat(context.actions()).filteredOn(action -> "enable".equals(action.actionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.authorized()).isFalse();
                    assertThat(action.authorizationDecision())
                            .isEqualTo(PlatformModuleRuntimeContextService.DECISION_ACCESS_DENIED);
                });
        assertThat(context.capabilities()).contains(EntityCapability.ENABLE);
        assertThat(context.abilities()).contains("enable");
    }

    @Test
    void shouldMergePersistedDynamicActionsWithDescriptorActions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", "合同", ModuleKind.DYNAMIC));
        PlatformModuleAction persistedUpdate = action("sales.contract", PlatformAction.UPDATE);
        persistedUpdate.setTitle("编辑合同");
        PlatformModuleAction disabledDelete = action("sales.contract", PlatformAction.DELETE);
        disabledDelete.setEnabled(Boolean.FALSE);
        when(actionService.listByModuleAliases(List.of("sales.contract")))
                .thenReturn(List.of(persistedUpdate, disabledDelete));
        when(dynamicRecordService.describe("sales.contract")).thenReturn(new DynamicModuleDescriptor(
                "sales.contract",
                "合同",
                "contract",
                List.of(
                        dynamicAction(PlatformAction.VIEW),
                        dynamicAction(PlatformAction.UPDATE),
                        dynamicAction(PlatformAction.DELETE)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        PlatformModuleRuntimeContextService service = new PlatformModuleRuntimeContextService(
                moduleService,
                actionService,
                new StaticModuleDefinitionCatalog(List.of()),
                dynamicRecordService,
                allowAllPolicy()
        );

        PlatformModuleRuntimeContext context = service.context("sales.contract");

        assertThat(context.actions()).extracting(PlatformModuleRuntimeAction::actionCode)
                .containsExactly("view", "update");
        assertThat(context.actions()).filteredOn(action -> "update".equals(action.actionCode()))
                .singleElement()
                .satisfies(action -> assertThat(action.title()).isEqualTo("编辑合同"));
    }

    @Test
    void shouldNotExposeSecondaryDynamicEntityCapabilitiesAsModuleAbilities() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", "合同", ModuleKind.DYNAMIC));
        when(actionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        when(dynamicRecordService.describe("sales.contract")).thenReturn(new DynamicModuleDescriptor(
                "sales.contract",
                "合同",
                "contract",
                List.of(dynamicAction(PlatformAction.VIEW)),
                List.of(
                        dynamicEntity("contract", "CRUD"),
                        dynamicEntity("contractLine", "CRUD", "TREE", "ENABLE")
                ),
                List.of(new DynamicRelationDescriptor(
                        "subLines", "contractLine", "contractSubLine", "contractLineId", false, false)),
                List.of(dynamicReference("contractLine", "productId")),
                List.of()
        ));
        PlatformModuleRuntimeContextService service = new PlatformModuleRuntimeContextService(
                moduleService,
                actionService,
                new StaticModuleDefinitionCatalog(List.of()),
                dynamicRecordService,
                allowAllPolicy()
        );

        PlatformModuleRuntimeContext context = service.context("sales.contract");

        assertThat(context.mainEntityAlias()).isEqualTo("contract");
        assertThat(context.abilities()).contains("crud");
        assertThat(context.abilities()).doesNotContain("tree", "enable", "childRelation", "reference",
                "referenceDependency");
    }


    private PlatformModule module(String alias, String title, ModuleKind moduleKind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setTitle(title);
        module.setModuleKind(moduleKind);
        module.setEntryType(ModuleEntryType.ROUTE);
        module.setEntryRoute("/iam/organizations");
        return module;
    }

    private EntityDefinition entity(String alias, Set<EntityCapability> capabilities) {
        return new EntityDefinition(alias, alias, alias, List.of(FieldDefinition.titleField()), capabilities);
    }

    private PlatformModuleAction action(String moduleAlias, PlatformAction platformAction) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias(moduleAlias);
        action.setActionCode(platformAction.code());
        action.setPermissionActionCode(platformAction.permissionActionCode());
        action.setTitle(platformAction.title());
        action.setCategory(EntityActionCategory.STANDARD);
        action.setActionLevel(EntityActionLevel.valueOf(platformAction.level().name()));
        action.setAccessMode(EntityActionAccessMode.valueOf(platformAction.accessMode().name()));
        action.setActionAuth(platformAction.actionAuth());
        action.setDataAuth(platformAction.dataAuth());
        action.setDefaultGrantPolicy(platformAction.defaultGrantPolicy());
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private DynamicActionDescriptor dynamicAction(PlatformAction platformAction) {
        return new DynamicActionDescriptor(
                platformAction.code(),
                platformAction.title(),
                true,
                EntityActionLevel.valueOf(platformAction.level().name()),
                EntityActionCategory.STANDARD,
                EntityActionAccessMode.valueOf(platformAction.accessMode().name()),
                platformAction.actionAuth(),
                platformAction.dataAuth(),
                platformAction.defaultGrantPolicy(),
                platformAction.inheritActionCode(),
                false,
                null,
                EntityActionExecutorType.STANDARD,
                null
        );
    }

    private DynamicEntityDescriptor dynamicEntity(String entityAlias, String... capabilities) {
        return new DynamicEntityDescriptor(
                entityAlias,
                entityAlias,
                Set.of(capabilities),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DynamicReferenceDescriptor dynamicReference(String sourceEntityAlias, String sourceField) {
        return new DynamicReferenceDescriptor(
                sourceEntityAlias,
                sourceField,
                "base.product",
                "product",
                ReferenceCardinality.ONE,
                true,
                null,
                List.of()
        );
    }

    private ActionExecutionPolicyService allowAllPolicy() {
        return context -> {
        };
    }
}
