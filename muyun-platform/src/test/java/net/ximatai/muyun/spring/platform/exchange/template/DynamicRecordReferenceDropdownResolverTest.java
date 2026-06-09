package net.ximatai.muyun.spring.platform.exchange.template;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicRecordReferenceDropdownResolverTest {
    private static final String MODULE = "sales.order";

    private final DynamicRecordService recordService = mock(DynamicRecordService.class);
    private final DynamicRecordReferenceDropdownResolver resolver =
            new DynamicRecordReferenceDropdownResolver(recordService);

    @Test
    void shouldReturnEmptyOptionsWhenReferenceTitlesAreDuplicated() {
        when(recordService.referenceOptions(eq(MODULE), eq("customer"), any(Criteria.class), any(PageRequest.class)))
                .thenReturn(PageResult.of(List.of(
                        new ReferenceOption("customer-1", "Acme"),
                        new ReferenceOption("customer-2", "Acme")
                ), 2, PageRequest.of(1, 501)));

        assertThat(resolver.resolve(reference(), 500)).isEmpty();
    }

    @Test
    void shouldReturnTitlesWhenReferenceTitlesAreUnique() {
        when(recordService.referenceOptions(eq(MODULE), eq("customer"), any(Criteria.class), any(PageRequest.class)))
                .thenReturn(PageResult.of(List.of(
                        new ReferenceOption("customer-1", "Acme"),
                        new ReferenceOption("customer-2", "Globex")
                ), 2, PageRequest.of(1, 501)));

        assertThat(resolver.resolve(reference(), 500)).containsExactly("Acme", "Globex");
    }

    private DynamicReferenceDescriptor reference() {
        return new DynamicReferenceDescriptor(
                "order",
                "customerId",
                MODULE,
                "customer",
                ReferenceCardinality.ONE,
                false,
                "",
                List.of()
        );
    }
}
