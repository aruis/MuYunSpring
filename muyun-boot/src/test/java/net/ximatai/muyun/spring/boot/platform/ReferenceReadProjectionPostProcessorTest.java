package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
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
        when(target.projections(eq(List.of("customer-1", "customer-2")), eq(List.of("title", "level"))))
                .thenReturn(Map.of(
                        "customer-1", Map.of("title", "客户一", "level", "A"),
                        "customer-2", Map.of("title", "客户二", "level", "B")
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
        verify(target).projections(List.of("customer-1", "customer-2"), List.of("title", "level"));
    }

    @Test
    void shouldStripInternalReadFieldsEvenWhenNoReferenceProjectionIsRequested() {
        List<Map<String, Object>> result = ReferenceReadProjectionPostProcessor.apply(PlainRecord.class, List.of(
                Map.of("id", "record-1", "version", 2, "tenantId", "tenant-a", "title", "Visible")
        ), List.of("title"));

        assertThat(result).containsExactly(Map.of("id", "record-1", "version", 2, "title", "Visible"));
    }

    @Test
    void shouldStripInternalReadFieldsWhenTheModelIsUnavailable() {
        List<Map<String, Object>> result = ReferenceReadProjectionPostProcessor.apply(null, List.of(
                Map.of("id", "record-1", "version", 2, "tenantId", "tenant-a", "title", "Visible")
        ), List.of("title"));

        assertThat(result).containsExactly(Map.of("id", "record-1", "version", 2, "title", "Visible"));
    }

    private static final class StaticOrder {
        @ReferenceTo(moduleAlias = "crm", entityAlias = "customer")
        private String customerId;

        @ReferenceLoad(source = "customerId", field = "title")
        private transient String customerTitle;

        @ReferenceLoad(source = "customerId", field = "level")
        private transient String customerLevel;
    }

    private static final class PlainRecord {
        private String title;
    }
}
