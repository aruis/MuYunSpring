package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.event.ModuleExtension;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandler;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeEventHandlerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MuYunSpringRuntimeEventHandlerConfiguration.class,
                    RuntimeEventPublisherConfiguration.class,
                    TestExtensionConfiguration.class
            );

    @Test
    void shouldRegisterModuleExtensionAsRuntimeEventListener() {
        contextRunner.run(context -> {
            RuntimeEventHandlerRegistry registry = context.getBean(RuntimeEventHandlerRegistry.class);
            ContractExtension extension = context.getBean(ContractExtension.class);

            assertThat(registry.descriptors()).hasSize(1);

            context.getBean(RuntimeEventPublisher.class).publish(event());

            assertThat(extension.count).isEqualTo(1);
        });
    }

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
    }

    @Configuration(proxyBeanMethods = false)
    static class RuntimeEventPublisherConfiguration {
        @Bean
        RuntimeEventPublisher runtimeEventPublisher(ObjectProvider<RuntimeEventListener> listeners) {
            return new RuntimeEventMulticaster(() -> listeners.orderedStream().toList());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestExtensionConfiguration {
        @Bean
        ContractExtension contractExtension() {
            return new ContractExtension();
        }
    }

    @ModuleExtension("sales.contract")
    static class ContractExtension {
        private int count;

        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE, entityAlias = "contract")
        void afterCreate(RuntimeEvent event) {
            count++;
        }
    }
}
