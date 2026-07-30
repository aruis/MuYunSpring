package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.security.FieldCryptoProvider;
import net.ximatai.muyun.spring.ability.security.FieldSigner;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicEntityServiceReferenceReadTest {
    @AfterEach
    void tearDown() {
        PlatformAbilityRuntime.resetReferenceTargetResolver();
    }

    @Test
    void shouldBatchDynamicListReferenceReadsFromAStaticTarget() {
        EntityDefinition contract = new EntityDefinition("contract", "sales_contract", "合同",
                List.of(FieldDefinition.string("title", "标题")), java.util.Set.of(EntityCapability.REFERENCE));
        EntityDefinition line = new EntityDefinition("line", "sales_line", "明细",
                List.of(FieldDefinition.string("contractId", "合同").column("contract_id")));
        ModuleDefinition module = ModuleDefinition.builder("sales.order", "订单")
                .entities(List.of(contract, line))
                .references(List.of(EntityReferenceDefinition.to("line", "contractId", ReferenceTarget.of("sales.order", "contract"))
                        .withAutoTitle("contractTitle")
                        .withProjection("title", "contractName")))
                .build();
        DynamicRecord first = new DynamicRecord(line).setValue("contractId", "contract-1");
        DynamicRecord second = new DynamicRecord(line).setValue("contractId", "contract-2");
        DynamicRecordDao sourceDao = mock(DynamicRecordDao.class);
        when(sourceDao.getEntity()).thenReturn(line);
        when(sourceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of(first, second));

        @SuppressWarnings("unchecked")
        ReferenceAbility targetAbility = mock(ReferenceAbility.class);
        when(targetAbility.titles(List.of("contract-1", "contract-2")))
                .thenReturn(Map.of("contract-1", "合同一", "contract-2", "合同二"));
        when(targetAbility.projections(List.of("contract-1", "contract-2"), List.of("title")))
                .thenReturn(Map.of("contract-1", Map.of("title", "合同一"),
                        "contract-2", Map.of("title", "合同二")));
        PlatformAbilityRuntime.configureReferenceTargetResolver(target ->
                ReferenceTarget.of("sales.order", "contract").equals(target)
                        ? java.util.Optional.of(targetAbility)
                        : java.util.Optional.empty());

        DynamicEntityService service = new DynamicEntityService(sourceDao, "sales.order", DynamicRecordLifecycle.NONE,
                module, ignored -> { throw new IllegalStateException("relations are not used"); },
                ignored -> { throw new IllegalStateException("target is static"); }, null, DynamicFieldValueValidator.NONE,
                FieldCryptoProvider.UNAVAILABLE, FieldSigner.UNAVAILABLE, new PlatformTimeService());

        List<DynamicRecord> records = service.list(Criteria.of(), PageRequest.of(1, 20));

        assertThat(records).extracting(record -> record.getValue("contractTitle"))
                .containsExactly("合同一", "合同二");
        verify(targetAbility).titles(List.of("contract-1", "contract-2"));
        verify(targetAbility).projections(List.of("contract-1", "contract-2"), List.of("title"));
    }
}
