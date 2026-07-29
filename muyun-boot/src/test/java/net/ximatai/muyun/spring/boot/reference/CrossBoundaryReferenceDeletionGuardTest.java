package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetDeletionPolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetProvider;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .beforeSoftDelete(dynamicTarget, target))
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

        new DynamicReferenceDeletionGuard(runtime).beforeSoftDelete(staticTarget, target);

        verify(runtime).validateReferenceTargetDeletion(
                eq(ReferenceTarget.of("education", "student")), eq("student-1"));
    }

    static final class StaticContractLink {
        @ReferenceTo(
                moduleAlias = "sales.contract",
                entityAlias = "contract",
                integrity = @ReferenceIntegrity(onTargetSoftDelete = ReferenceTargetDeletionPolicy.RESTRICT)
        )
        private String contractId;
    }
}
