package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicRelationProjectionReadServiceTest {
    @Test
    void shouldQueryDynamicUiListThroughUnifiedRelationProjection() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        DynamicRelationProjectionReadService service = new DynamicRelationProjectionReadService(
                new RelationProjectionReadService(
                        new RelationProjectionQueryExecutor(jdbcOperations),
                        new RelationProjectionDatabaseTypeProvider()
                )
        );
        DynamicRecordService recordService = dynamicRecordService();
        when(jdbcOperations.queryForList(any(String.class), any(Map.class)))
                .thenReturn(List.of(Map.of(
                        "id", "order-1",
                        "tenantId", "tenant_a",
                        "version", 7,
                        "orderNo", "O-001",
                        "customerTitle", "Acme"
                )));
        when(jdbcOperations.queryForObject(any(String.class), any(Map.class), eq(Long.class)))
                .thenReturn(1L);

        PageResult<DynamicRecord> page = service.queryList(
                "crm.order",
                recordService,
                Set.of("orderNo", "customerTitle"),
                Criteria.of(),
                PageRequest.of(1, 20)
        ).orElseThrow();

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).singleElement().satisfies(record -> {
            assertThat(record.getId()).isEqualTo("order-1");
            assertThat(record.getTenantId()).isEqualTo("tenant_a");
            assertThat(record.getVersion()).isEqualTo(7);
            assertThat(record.getValue("orderNo")).isEqualTo("O-001");
            assertThat(record.getValue("customerTitle")).isEqualTo("Acme");
        });
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(jdbcOperations).queryForList(sqlCaptor.capture(), any(Map.class));
        assertThat(sqlCaptor.getValue())
                .contains("left join \"public\".\"crm_customer\" \"customer_id\"")
                .contains("\"tenantId\"", "\"version\"")
                .contains("\"customer_id\".\"title\" as \"customerTitle\"");
    }

    @Test
    void shouldFallbackWhenDynamicProjectionTouchesProtectedFields() {
        DynamicRelationProjectionReadService service = new DynamicRelationProjectionReadService(
                new RelationProjectionReadService(
                        new RelationProjectionQueryExecutor(mock(NamedParameterJdbcOperations.class)),
                        new RelationProjectionDatabaseTypeProvider()
                )
        );

        assertThat(service.supportsListQuery(
                "crm.order",
                protectedDynamicRecordService(),
                Set.of("orderNo", "customerTitle")
        )).isFalse();
    }

    @Test
    void shouldFallbackWhenDynamicProjectionCannotProduceAllOutputFields() {
        DynamicRelationProjectionReadService service = new DynamicRelationProjectionReadService(
                new RelationProjectionReadService(
                        new RelationProjectionQueryExecutor(mock(NamedParameterJdbcOperations.class)),
                        new RelationProjectionDatabaseTypeProvider()
                )
        );

        assertThat(service.supportsListQuery(
                "crm.order",
                dynamicRecordService(),
                Set.of("orderNo", "customerTitle", "displayCode")
        )).isFalse();
    }

    @Test
    void shouldApplyDynamicReadScopeBeforeRelationProjectionSqlExecution() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        DynamicRelationProjectionReadService service = new DynamicRelationProjectionReadService(
                new RelationProjectionReadService(
                        new RelationProjectionQueryExecutor(jdbcOperations),
                        new RelationProjectionDatabaseTypeProvider()
                )
        );
        when(jdbcOperations.queryForList(any(String.class), any(Map.class)))
                .thenReturn(List.of(Map.of(
                        "id", "order-1",
                        "tenantId", "tenant_a",
                        "orderNo", "O-001",
                        "customerTitle", "Acme"
                )));
        when(jdbcOperations.queryForObject(any(String.class), any(Map.class), eq(Long.class)))
                .thenReturn(1L);

        service.queryList(
                "crm.order",
                scopedDynamicRecordService(),
                Set.of("orderNo", "customerTitle"),
                Criteria.of(),
                PageRequest.of(1, 20)
        ).orElseThrow();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(jdbcOperations).queryForList(sqlCaptor.capture(), paramsCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("\"main\".\"tenant_id\" as \"tenantId\"")
                .contains("where \"tenantId\" = :");
        assertThat(paramsCaptor.getValue()).containsValue("tenant_a");
    }

    private DynamicRecordService dynamicRecordService() {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(mock(IDatabaseOperations.class));
        runtime.register(orderModule());
        runtime.register(customerModule());
        return new DynamicRecordService(runtime);
    }

    private DynamicRecordService scopedDynamicRecordService() {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(mock(IDatabaseOperations.class));
        runtime.register(orderModule());
        runtime.register(customerModule());
        return new DynamicRecordService(
                runtime,
                new AllowAllActionExecutionPolicyService(),
                new TenantRestrictedDataScopeCriteriaService()
        );
    }

    private DynamicRecordService protectedDynamicRecordService() {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(mock(IDatabaseOperations.class));
        runtime.register(orderModule());
        runtime.register(protectedCustomerModule());
        return new DynamicRecordService(runtime);
    }

    private static final class TenantRestrictedDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.restricted(Criteria.of().eq("tenantId", "tenant_a"));
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.restricted(Criteria.of().eq("tenantId", "tenant_a"));
        }
    }

    private ModuleDefinition orderModule() {
        return new ModuleDefinition(
                "crm.order",
                "订单",
                List.of(new EntityDefinition(
                        "order",
                        "crm_order",
                        "Order",
                        List.of(
                                FieldDefinition.string("customerId", "客户").column("customer_id"),
                                FieldDefinition.string("orderNo", "订单号").column("order_no")
                        )
                )),
                List.of(),
                List.of(new EntityReferenceDefinition(
                        "order",
                        "customerId",
                        "crm.customer.customer",
                        ReferenceCardinality.ONE,
                        false,
                        "",
                        List.of(new ReferenceProjection("title", "customerTitle"))
                ))
        );
    }

    private ModuleDefinition customerModule() {
        return new ModuleDefinition(
                "crm.customer",
                "客户",
                List.of(new EntityDefinition(
                        "customer",
                        "crm_customer",
                        "Customer",
                        List.of(FieldDefinition.string("title", "客户名称").column("title"))
                ))
        );
    }

    private ModuleDefinition protectedCustomerModule() {
        return new ModuleDefinition(
                "crm.customer",
                "客户",
                List.of(new EntityDefinition(
                        "customer",
                        "crm_customer",
                        "Customer",
                        List.of(FieldDefinition.string("title", "客户名称")
                                .column("title")
                                .protection(masked()))
                ))
        );
    }

    private FieldProtectionDefinition masked() {
        return new FieldProtectionDefinition(
                FieldEncryptionMode.NONE,
                FieldSignatureMode.NONE,
                FieldMaskingPolicy.MIDDLE
        );
    }
}
