package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionTransactionOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class MuYunSpringDynamicRuntimeConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BaseConfig.class)
            .withUserConfiguration(MuYunSpringDynamicRuntimeConfiguration.class);

    @Test
    void shouldConfigureLazyRuntimeEventMulticasterWhenListenersExist() {
        List<String> received = new ArrayList<>();

        contextRunner.withBean("firstRuntimeEventListener", RuntimeEventListener.class,
                        () -> event -> received.add("first:" + event.eventType()))
                .withBean("secondRuntimeEventListener", RuntimeEventListener.class,
                        () -> event -> received.add("second:" + event.eventType()))
                .run(context -> {
                    RuntimeEventPublisher publisher = context.getBean(RuntimeEventPublisher.class);

                    publisher.publish(event());

                    assertThat(publisher).isInstanceOf(RuntimeEventMulticaster.class);
                    assertThat(received).containsExactly("first:AFTER_CREATE", "second:AFTER_CREATE");
                });
    }

    @Test
    void shouldKeepRuntimeEventPublisherNoopWhenNoListenerExists() {
        contextRunner.run(context -> {
            RuntimeEventPublisher publisher = context.getBean(RuntimeEventPublisher.class);

            publisher.publish(event());

            assertThat(publisher).isInstanceOf(RuntimeEventMulticaster.class);
            assertThat(((RuntimeEventMulticaster) publisher).listeners()).isEmpty();
        });
    }

    @Test
    void shouldRespectCustomRuntimeEventPublisher() {
        RuntimeEventPublisher customPublisher = event -> {
        };

        contextRunner.withBean(RuntimeEventPublisher.class, () -> customPublisher)
                .run(context -> assertThat(context.getBean(RuntimeEventPublisher.class)).isSameAs(customPublisher));
    }

    @Test
    void shouldDispatchRuntimeEventsBySpringOrder() {
        contextRunner.withUserConfiguration(OrderedListenerConfig.class)
                .run(context -> {
                    OrderedRecorder recorder = context.getBean(OrderedRecorder.class);

                    context.getBean(RuntimeEventPublisher.class).publish(event());

                    assertThat(recorder.received()).containsExactly("first", "second");
                });
    }

    @Test
    void shouldAllowRuntimeEventListenerToDependOnDynamicRecordRuntime() {
        contextRunner.withUserConfiguration(RuntimeDependentListenerConfig.class)
                .run(context -> {
                    OrderedRecorder recorder = context.getBean(OrderedRecorder.class);

                    context.getBean(RuntimeEventPublisher.class).publish(event());

                    assertThat(context).hasSingleBean(DynamicRecordRuntime.class);
                    assertThat(recorder.received()).containsExactly("runtime-listener");
                });
    }

    @Test
    void shouldConfigureDynamicActionExecutorRegistryFromExecutorBeans() {
        contextRunner.withBean(DynamicActionExecutor.class, () -> new TestActionExecutor("contractSubmit"))
                .run(context -> assertThat(context.getBean(DynamicActionExecutorRegistry.class)
                        .contains("contractSubmit")).isTrue());
    }

    @Test
    void shouldRespectCustomDynamicActionExecutorRegistry() {
        DynamicActionExecutorRegistry customRegistry = new DynamicActionExecutorRegistry(List.of(
                new TestActionExecutor("customSubmit")
        ));

        contextRunner.withBean(DynamicActionExecutorRegistry.class, () -> customRegistry)
                .run(context -> assertThat(context.getBean(DynamicActionExecutorRegistry.class)).isSameAs(customRegistry));
    }

    @Test
    void shouldAllowDynamicActionExecutorToDependOnDynamicRecordRuntime() {
        contextRunner.withUserConfiguration(RuntimeDependentExecutorConfig.class)
                .run(context -> {
                    DynamicActionExecutorRegistry registry = context.getBean(DynamicActionExecutorRegistry.class);

                    assertThat(context).hasSingleBean(DynamicRecordRuntime.class);
                    assertThat(registry.contains("runtimeSubmit")).isTrue();
                });
    }

    @Test
    void shouldComposeDynamicRecordMutationCoordinatorsBySpringOrder() {
        contextRunner.withUserConfiguration(OrderedMutationCoordinatorConfig.class)
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    IDatabaseOperations<Object> operations = context.getBean(IDatabaseOperations.class);
                    when(operations.insertItem(eq("public"), eq("app_contract"), anyMap()))
                            .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
                    when(operations.patchUpdateItemWhere(eq("public"), eq("app_contract"), anyMap(), anyMap()))
                            .thenReturn(1);
                    when(operations.query(anyString(), anyMap()))
                            .thenReturn(List.of(contractRow("contract-1")));
                    DynamicRecordRuntime runtime = context.getBean(DynamicRecordRuntime.class);
                    runtime.register(new ModuleDefinition("sales.contract", "Contract", List.of(contractEntity())));
                    DynamicRecordService service = context.getBean(DynamicRecordService.class);
                    OrderedRecorder recorder = context.getBean(OrderedRecorder.class);
                    DynamicRecord record = service.newRecord("sales.contract", "contract")
                            .setValue("code", "C-001");
                    record.setId("contract-1");

                    service.create("sales.contract", "contract", record);
                    service.update("sales.contract", "contract", record);
                    service.delete("sales.contract", "contract", "contract-1");

                    assertThat(recorder.received()).containsExactly(
                            "first:beforeCreate", "second:beforeCreate",
                            "first:afterCreate", "second:afterCreate",
                            "first:beforeUpdate", "second:beforeUpdate",
                            "first:afterUpdate", "second:afterUpdate",
                            "first:beforeDelete", "second:beforeDelete",
                            "first:afterDelete", "second:afterDelete"
                    );
                });
    }

    @Test
    void shouldConfigureActionTransactionOperatorFromTransactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        contextRunner.withBean(PlatformTransactionManager.class, () -> transactionManager)
                .run(context -> {
                    DynamicActionTransactionOperator operator = context.getBean(DynamicActionTransactionOperator.class);

                    String result = operator.executeResult(null, () -> "ok");

                    assertThat(result).isEqualTo("ok");
                    verify(transactionManager).getTransaction(any());
                    verify(transactionManager).commit(any());
                });
    }

    @Test
    void shouldRollbackActionTransactionWhenCallbackFails() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        contextRunner.withBean(PlatformTransactionManager.class, () -> transactionManager)
                .run(context -> {
                    DynamicActionTransactionOperator operator = context.getBean(DynamicActionTransactionOperator.class);

                    assertThatThrownBy(() -> operator.execute(null, () -> {
                        throw new IllegalStateException("boom");
                    })).isInstanceOf(IllegalStateException.class);

                    verify(transactionManager).getTransaction(any());
                    verify(transactionManager).rollback(any());
                });
    }

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition("contract", "app_contract", "Contract",
                List.of(FieldDefinition.string("code", "Code").length(64)));
    }

    private Map<String, Object> contractRow(String id) {
        return Map.of(
                "id", id,
                "code", "C-001",
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class BaseConfig {
        @Bean
        @SuppressWarnings("unchecked")
        IDatabaseOperations<Object> databaseOperations() {
            IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
            when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
            when(operations.getDefaultSchemaName()).thenReturn("public");
            return operations;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedListenerConfig {
        @Bean
        OrderedRecorder orderedRecorder() {
            return new OrderedRecorder();
        }

        @Bean
        @Order(2)
        RuntimeEventListener secondRuntimeEventListener(OrderedRecorder recorder) {
            return event -> recorder.add("second");
        }

        @Bean
        @Order(1)
        RuntimeEventListener firstRuntimeEventListener(OrderedRecorder recorder) {
            return event -> recorder.add("first");
        }
    }

    static class OrderedRecorder {
        private final List<String> received = new ArrayList<>();

        void add(String value) {
            received.add(value);
        }

        List<String> received() {
            return received;
        }
    }

    private record TestActionExecutor(String executorKey) implements DynamicActionExecutor {
        @Override
        public Object execute(net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext context,
                              net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest request) {
            return null;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RuntimeDependentListenerConfig {
        @Bean
        OrderedRecorder runtimeDependentRecorder() {
            return new OrderedRecorder();
        }

        @Bean
        RuntimeEventListener runtimeDependentListener(DynamicRecordRuntime runtime, OrderedRecorder recorder) {
            return event -> {
                assertThat(runtime).isNotNull();
                recorder.add("runtime-listener");
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RuntimeDependentExecutorConfig {
        @Bean
        DynamicActionExecutor runtimeDependentActionExecutor(DynamicRecordRuntime runtime) {
            assertThat(runtime).isNotNull();
            return new TestActionExecutor("runtimeSubmit");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedMutationCoordinatorConfig {
        @Bean
        OrderedRecorder mutationRecorder() {
            return new OrderedRecorder();
        }

        @Bean
        @Order(2)
        DynamicRecordMutationCoordinator secondMutationCoordinator(OrderedRecorder mutationRecorder) {
            return new RecordingMutationCoordinator("second", mutationRecorder);
        }

        @Bean
        @Order(1)
        DynamicRecordMutationCoordinator firstMutationCoordinator(OrderedRecorder mutationRecorder) {
            return new RecordingMutationCoordinator("first", mutationRecorder);
        }
    }

    private record RecordingMutationCoordinator(String name, OrderedRecorder recorder)
            implements DynamicRecordMutationCoordinator {
        @Override
        public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
            recorder.add(name + ":beforeCreate");
        }

        @Override
        public void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
            recorder.add(name + ":afterCreate");
        }

        @Override
        public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
            recorder.add(name + ":beforeUpdate");
        }

        @Override
        public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
            recorder.add(name + ":afterUpdate");
        }

        @Override
        public void beforeDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            recorder.add(name + ":beforeDelete");
        }

        @Override
        public void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            recorder.add(name + ":afterDelete");
        }
    }
}
