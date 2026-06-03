package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformActionResolverTest {
    @Test
    void shouldDeriveStandardActionsFromEntityCapabilities() {
        EntityDefinition entity = new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code"),
                FieldDefinition.titleField().queryable(),
                FieldDefinition.parentId(),
                FieldDefinition.sortOrder(),
                FieldDefinition.enabled()
        ), Set.of(EntityCapability.TREE, EntityCapability.REFERENCE, EntityCapability.ENABLE));

        assertThat(EntityStandardActionCatalog.from(entity))
                .extracting(EntityActionDefinition::actionCode)
                .containsExactlyElementsOf(Arrays.stream(PlatformAction.values())
                        .map(PlatformAction::code)
                        .toList());
    }

    @Test
    void shouldExposeStandardActionsFromSupportedActionGroupsOnly() {
        assertThat(actionCodes(entityWith(EntityCapability.CRUD)))
                .containsExactlyElementsOf(actionCodes(PlatformActionGroup.CRUD));
        assertThat(actionCodes(entityWith(EntityCapability.SORT)))
                .containsExactlyElementsOf(actionCodes(PlatformActionGroup.CRUD, PlatformActionGroup.SORT));
        assertThat(actionCodes(entityWith(EntityCapability.TREE)))
                .containsExactlyElementsOf(actionCodes(PlatformActionGroup.CRUD, PlatformActionGroup.SORT,
                        PlatformActionGroup.TREE));
        assertThat(actionCodes(entityWith(EntityCapability.REFERENCE)))
                .containsExactlyElementsOf(actionCodes(PlatformActionGroup.CRUD, PlatformActionGroup.REFERENCE));
        assertThat(actionCodes(entityWith(EntityCapability.ENABLE)))
                .containsExactlyElementsOf(actionCodes(PlatformActionGroup.CRUD, PlatformActionGroup.ENABLE));
    }

    private EntityDefinition entityWith(EntityCapability capability) {
        return new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code"),
                FieldDefinition.titleField(),
                FieldDefinition.parentId(),
                FieldDefinition.sortOrder(),
                FieldDefinition.enabled()
        )).withCapabilities(capability);
    }

    private List<String> actionCodes(EntityDefinition entity) {
        return EntityStandardActionCatalog.from(entity).stream()
                .map(EntityActionDefinition::actionCode)
                .toList();
    }

    private List<String> actionCodes(PlatformActionGroup firstGroup, PlatformActionGroup... otherGroups) {
        List<PlatformActionGroup> groups = new ArrayList<>();
        groups.add(firstGroup);
        groups.addAll(List.of(otherGroups));
        return Arrays.stream(PlatformAction.values())
                .filter(action -> groups.contains(action.group()))
                .map(PlatformAction::code)
                .toList();
    }
}
