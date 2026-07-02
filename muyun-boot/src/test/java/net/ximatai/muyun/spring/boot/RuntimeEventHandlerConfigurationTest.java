package net.ximatai.muyun.spring.boot;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import net.ximatai.muyun.spring.ability.event.ModuleExtension;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandler;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeEventHandlerConfigurationTest {
    private final MuYunSpringRuntimeEventHandlerConfiguration configuration =
            new MuYunSpringRuntimeEventHandlerConfiguration();

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRegisterModuleExtensionAsRuntimeEventListener() {
        ContractExtension extension = new ContractExtension();
        Bean bean = mock(Bean.class);
        CreationalContext<ContractExtension> creationalContext = mock(CreationalContext.class);
        BeanManager beanManager = mock(BeanManager.class);
        when(bean.getBeanClass()).thenReturn(ContractExtension.class);
        when(beanManager.getBeans(Object.class, Any.Literal.INSTANCE)).thenReturn(Set.of(bean));
        when(beanManager.createCreationalContext(bean)).thenReturn(creationalContext);
        when(beanManager.getReference(eq(bean), eq(ContractExtension.class), any(CreationalContext.class)))
                .thenReturn(extension);

        RuntimeEventHandlerRegistry registry = configuration.runtimeEventHandlerRegistry(beanManager);
        RuntimeEventListener listener = configuration.runtimeEventHandlerListener(registry);

        listener.onRuntimeEvent(event());

        assertThat(registry.descriptors()).hasSize(1);
        assertThat(extension.count).isEqualTo(1);
    }

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
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
