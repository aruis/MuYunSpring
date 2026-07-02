package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.di.ObjectProviders;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.schema.PlatformDatabaseValueConverter;
import net.ximatai.muyun.spring.common.schema.PlatformSchemaMigrationPolicy;
import net.ximatai.muyun.spring.common.time.BusinessCalendarService;
import net.ximatai.muyun.spring.common.time.BusinessTimeContext;
import net.ximatai.muyun.spring.common.time.BusinessTimeZoneResolver;
import net.ximatai.muyun.spring.common.time.NaturalBusinessCalendarService;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuYunSpringDynamicRuntimeConfigurationTest {
    private final MuYunSpringDynamicRuntimeConfiguration configuration =
            new MuYunSpringDynamicRuntimeConfiguration();

    @Test
    void shouldConfigureLazyRuntimeEventMulticasterWhenListenersExist() {
        List<String> received = new ArrayList<>();

        RuntimeEventPublisher publisher = configuration.runtimeEventPublisher(ObjectProviders.of(List.of(
                event -> received.add("first:" + event.eventType()),
                event -> received.add("second:" + event.eventType())
        )));

        publisher.publish(event());

        assertThat(publisher).isInstanceOf(RuntimeEventMulticaster.class);
        assertThat(received).containsExactly("first:AFTER_CREATE", "second:AFTER_CREATE");
    }

    @Test
    void shouldKeepRuntimeEventPublisherNoopWhenNoListenerExists() {
        RuntimeEventPublisher publisher = configuration.runtimeEventPublisher(ObjectProviders.of(List.<RuntimeEventListener>of()));

        publisher.publish(event());

        assertThat(publisher).isInstanceOf(RuntimeEventMulticaster.class);
        assertThat(((RuntimeEventMulticaster) publisher).listeners()).isEmpty();
    }

    @Test
    void shouldConfigureDynamicActionExecutorRegistryFromExecutorBeans() {
        DynamicActionExecutorRegistry registry = configuration.dynamicActionExecutorRegistry(
                ObjectProviders.of(List.of(new TestActionExecutor("contractSubmit")))
        );

        assertThat(registry.contains("contractSubmit")).isTrue();
        assertThat(registry.contains("missing")).isFalse();
    }

    @Test
    void shouldConfigurePlatformTimeServiceDefaultZone() {
        MuYunSpringPlatformTimeProperties properties = new MuYunSpringPlatformTimeProperties();
        properties.setDefaultZoneId("Asia/Shanghai");

        PlatformTimeService timeService = configuration.platformTimeService(
                ObjectProviders.of(Clock.fixed(Instant.parse("2026-06-17T00:00:00Z"), ZoneOffset.UTC)),
                ObjectProviders.of(List.of()),
                properties
        );

        assertThat(timeService.resolveZoneId(BusinessTimeContext.empty()))
                .isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void shouldRejectInvalidPlatformDefaultZone() {
        MuYunSpringPlatformTimeProperties properties = new MuYunSpringPlatformTimeProperties();
        properties.setDefaultZoneId("+08:00");

        assertThatThrownBy(() -> configuration.platformTimeService(
                ObjectProviders.of((Clock) null),
                ObjectProviders.of(List.of()),
                properties
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IANA");
    }

    @Test
    void shouldPreferBusinessTimeZoneResolverBeforeConfiguredDefaultZone() {
        MuYunSpringPlatformTimeProperties properties = new MuYunSpringPlatformTimeProperties();
        properties.setDefaultZoneId("Asia/Shanghai");
        BusinessTimeZoneResolver resolver = context -> "org-new-york".equals(context.organizationId())
                ? Optional.of(ZoneId.of("America/New_York"))
                : Optional.empty();

        PlatformTimeService timeService = configuration.platformTimeService(
                ObjectProviders.of((Clock) null),
                ObjectProviders.of(List.of(resolver)),
                properties
        );

        assertThat(timeService.resolveZoneId(BusinessTimeContext.ofOrganization("org-new-york")))
                .isEqualTo(ZoneId.of("America/New_York"));
        assertThat(timeService.resolveZoneId(BusinessTimeContext.ofOrganization("org-shanghai")))
                .isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void shouldConfigureNaturalBusinessCalendarServiceByDefault() {
        PlatformTimeService timeService = new PlatformTimeService(Clock.systemUTC(), ZoneId.of("UTC"), List.of());

        BusinessCalendarService calendarService = configuration.businessCalendarService(timeService);

        assertThat(calendarService).isInstanceOf(NaturalBusinessCalendarService.class);
    }

    @Test
    void shouldConfigureDynamicSchemaServiceWithRuntimeModePolicy() {
        DynamicSchemaService production = configuration.dynamicSchemaService(
                operations(),
                configuration.moduleDefinitionValidator(),
                () -> PlatformRuntimeMode.PRODUCTION
        );
        DynamicSchemaService development = configuration.dynamicSchemaService(
                operations(),
                configuration.moduleDefinitionValidator(),
                () -> PlatformRuntimeMode.DEVELOPMENT
        );

        assertThat(migrationPolicy(production).defaultOptions().isStrict()).isTrue();
        assertThat(migrationPolicy(development).defaultOptions().isStrict()).isFalse();
        assertThat(migrationPolicy(production).resolve(MigrationOptions.dryRun()).isDryRun()).isTrue();
    }

    @Test
    void shouldApplyConfiguredDefaultZoneToDynamicRuntimeQueries() {
        MuYunSpringPlatformTimeProperties properties = new MuYunSpringPlatformTimeProperties();
        properties.setDefaultZoneId("Asia/Shanghai");
        PlatformTimeService timeService = configuration.platformTimeService(
                ObjectProviders.of(Clock.fixed(Instant.parse("2026-06-17T00:00:00Z"), ZoneOffset.UTC)),
                ObjectProviders.of(List.of()),
                properties
        );
        DynamicRecordRuntime runtime = configuration.dynamicRecordRuntime(
                operations(),
                (moduleAlias, definition, field, value) -> {
                },
                event -> {
                },
                new DynamicActionExecutorRegistry(List.of()),
                (context, action) -> action.get(),
                ObjectProviders.of(null),
                ObjectProviders.of(null),
                timeService,
                valueConverter()
        );
        runtime.register(new ModuleDefinition("sales.contract", "Contract", List.of(timeContractEntity())));

        Criteria criteria = runtime.entityService("sales.contract", "contract")
                .queryCriteria(List.of(DynamicQueryCondition.of(
                        "submittedAt",
                        DynamicQueryOperator.BETWEEN,
                        "2026-01-01",
                        "2026-01-01"
                )));

        assertThat(criteria.getClauses()).hasSize(2);
        assertThat(criteria.getClauses().get(0).getOperator()).isEqualTo(CriteriaOperator.GTE);
        assertThat(criteria.getClauses().get(0).getValues())
                .containsExactly(Instant.parse("2025-12-31T16:00:00Z"));
        assertThat(criteria.getClauses().get(1).getOperator()).isEqualTo(CriteriaOperator.LT);
        assertThat(criteria.getClauses().get(1).getValues())
                .containsExactly(Instant.parse("2026-01-01T16:00:00Z"));
    }

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
    }

    private EntityDefinition timeContractEntity() {
        return new EntityDefinition("contract", "app_contract", "Contract",
                List.of(FieldDefinition.timestamp("submittedAt", "Submitted At").column("submitted_at").queryable(
                        DynamicQueryOperator.BETWEEN,
                        java.util.Set.of(DynamicQueryOperator.BETWEEN)
                )));
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn("public");
        return operations;
    }

    private DatabaseValueConverter valueConverter() {
        return new PlatformDatabaseValueConverter();
    }

    private PlatformSchemaMigrationPolicy migrationPolicy(DynamicSchemaService schemaService) {
        try {
            Field field = DynamicSchemaService.class.getDeclaredField("migrationPolicy");
            field.setAccessible(true);
            return (PlatformSchemaMigrationPolicy) field.get(schemaService);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("DynamicSchemaService migration policy is not inspectable", e);
        }
    }

    private record TestActionExecutor(String executorKey) implements DynamicActionExecutor {
        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            return null;
        }
    }
}
