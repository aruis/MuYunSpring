package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
    }

    @Configuration(proxyBeanMethods = false)
    static class BaseConfig {
        @Bean
        @SuppressWarnings("unchecked")
        IDatabaseOperations<Object> databaseOperations() {
            return mock(IDatabaseOperations.class);
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
}
