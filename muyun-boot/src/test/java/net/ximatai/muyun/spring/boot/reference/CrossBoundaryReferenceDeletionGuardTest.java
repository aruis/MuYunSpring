package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionMode;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;
import net.ximatai.muyun.spring.ability.deletion.DeletionResource;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetProvider;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class CrossBoundaryReferenceDeletionGuardTest {
    @Test
    void shouldRestrictDynamicTargetReferencedByStaticModel() {
        CrudAbility<?> dynamicTarget = mock(CrudAbility.class,
                withSettings().extraInterfaces(ReferenceTargetProvider.class));
        when(((ReferenceTargetProvider) dynamicTarget).referenceTarget())
                .thenReturn(ReferenceTarget.of("sales.contract", "contract"));
        EntityContract target = mock(EntityContract.class);
        when(target.getId()).thenReturn("contract-1");

        @SuppressWarnings("rawtypes")
        CrudAbility staticSource = mock(CrudAbility.class);
        when(staticSource.modelClass()).thenReturn(StaticContractLink.class);
        when(staticSource.getModuleAlias()).thenReturn("education.contract-link");
        when(staticSource.count(any(Criteria.class))).thenReturn(1L);

        assertThatThrownBy(() -> new StaticReferenceDeletionGuard(List.of(staticSource))
                .beforeTargetUnavailable(dynamicTarget, target))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("sales.contract.contract");
    }

    @Test
    void shouldCheckDynamicReferrersForStaticTarget() {
        DynamicRecordRuntime runtime = mock(DynamicRecordRuntime.class);
        CrudAbility<?> staticTarget = mock(CrudAbility.class);
        when(staticTarget.getModuleAlias()).thenReturn("education.student");
        EntityContract target = mock(EntityContract.class);
        when(target.getId()).thenReturn("student-1");

        new DynamicReferenceDeletionGuard(runtime).beforeTargetUnavailable(staticTarget, target);

        verify(runtime).validateReferenceTargetDeletion(
                eq(ReferenceTarget.of("education", "student")), eq("student-1"));
    }

    @Test
    void shouldCascadeDynamicReferrersWithTheParentDeletionNode() {
        DynamicRecordRuntime runtime = mock(DynamicRecordRuntime.class);
        CrudAbility<?> staticTarget = mock(CrudAbility.class);
        when(staticTarget.getModuleAlias()).thenReturn("education.student");
        EntityContract target = mock(EntityContract.class);
        when(target.getId()).thenReturn("student-1");
        DeletionContext context = DeletionContext.root("education.student", "student-1");
        DeletionNode node = DeletionNode.transientNode(new DeletionResource("education.student", "student-1"));

        new DynamicReferenceDeletionGuard(runtime).beforeTargetUnavailable(
                staticTarget, target, context, node, DeletionMode.HARD);

        verify(runtime).cascadeReferenceTargetUnavailable(
                eq(ReferenceTarget.of("education", "student")), eq("student-1"), eq(context), eq(node));
    }

    @Test
    void shouldCascadeStaticReferrerInSameDeletionContext() {
        CrudAbility<?> targetAbility = mock(CrudAbility.class);
        when(targetAbility.getModuleAlias()).thenReturn("education.classroom");
        EntityContract target = mock(EntityContract.class);
        when(target.getId()).thenReturn("classroom-1");

        @SuppressWarnings("rawtypes")
        CrudAbility source = mock(CrudAbility.class);
        EntityContract referrer = mock(EntityContract.class);
        when(referrer.getId()).thenReturn("member-1");
        when(referrer.getVersion()).thenReturn(3);
        when(source.modelClass()).thenReturn(CascadeLink.class);
        when(source.getModuleAlias()).thenReturn("education.class-member");
        when(source.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(referrer), List.of());

        DeletionContext context = DeletionContext.root("education.classroom", "classroom-1");
        DeletionNode node = DeletionNode.transientNode(new DeletionResource("education.classroom", "classroom-1"));
        new StaticReferenceDeletionGuard(List.of(source)).beforeTargetUnavailable(
                targetAbility, target, context, node, DeletionMode.SOFT);

        ArgumentCaptor<DeletionContext> childContext = ArgumentCaptor.forClass(DeletionContext.class);
        verify(source).delete(eq("member-1"), eq(3), childContext.capture());
        assertThat(childContext.getValue().operationId()).isEqualTo(context.operationId());
        assertThat(childContext.getValue().parent()).isEqualTo(node.resource());
        assertThat(childContext.getValue().trigger().name()).isEqualTo("CASCADE");
    }

    static final class StaticContractLink {
        @ReferenceTo(
                moduleAlias = "sales.contract",
                entityAlias = "contract",
                integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT)
        )
        private String contractId;
    }

    static final class CascadeLink {
        @ReferenceTo(
                moduleAlias = "education",
                entityAlias = "classroom",
                integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.CASCADE_DELETE)
        )
        private String classroomId;
    }
}
