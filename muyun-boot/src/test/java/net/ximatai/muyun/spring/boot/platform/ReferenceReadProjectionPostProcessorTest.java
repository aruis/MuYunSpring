package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceReadProjectionPostProcessorTest {
    @AfterEach
    void tearDown() {
        PlatformAbilityRuntime.resetReferenceTargetResolver();
    }

    @Test
    void shouldBatchEnrichStaticRecordsFromADynamicReferenceTarget() {
        @SuppressWarnings("unchecked")
        ReferenceAbility<?> target = mock(ReferenceAbility.class);
        ReferenceTarget customer = ReferenceTarget.of("crm", "customer");
        when(target.titles(List.of("customer-1", "customer-2")))
                .thenReturn(Map.of("customer-1", "客户一", "customer-2", "客户二"));
        when(target.projections(eq(List.of("customer-1", "customer-2")), eq(List.of("level"))))
                .thenReturn(Map.of(
                        "customer-1", Map.of("level", "A"),
                        "customer-2", Map.of("level", "B")
                ));
        PlatformAbilityRuntime.configureReferenceTargetResolver(reference -> customer.equals(reference)
                ? java.util.Optional.of(target)
                : java.util.Optional.empty());

        List<Map<String, Object>> result = ReferenceReadProjectionPostProcessor.apply(StaticOrder.class, List.of(
                Map.of("id", "order-1", "customerId", "customer-1"),
                Map.of("id", "order-2", "customerId", "customer-2")
        ));

        assertThat(result).containsExactly(
                Map.of("id", "order-1", "customerId", "customer-1", "customerTitle", "客户一", "customerLevel", "A"),
                Map.of("id", "order-2", "customerId", "customer-2", "customerTitle", "客户二", "customerLevel", "B")
        );
        verify(target).titles(List.of("customer-1", "customer-2"));
        verify(target).projections(List.of("customer-1", "customer-2"), List.of("level"));
    }

    private static final class StaticOrder {
        @ReferenceTo(moduleAlias = "crm", entityAlias = "customer", autoTitle = true, titleOutputField = "customerTitle",
                projections = @net.ximatai.muyun.spring.ability.reference.ReferenceProject(
                        targetField = "level", outputField = "customerLevel"))
        private String customerId;
    }
}
