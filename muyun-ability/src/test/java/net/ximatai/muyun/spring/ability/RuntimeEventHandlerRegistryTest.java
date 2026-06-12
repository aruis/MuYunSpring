package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.event.ModuleExtension;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandler;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerDescriptor;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerException;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerFailurePolicy;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerPhase;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeEventHandlerRegistryTest {
    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldScanAnnotatedModuleExtensionHandlersAndDispatchInOrder() {
        OrderedContractExtension extension = new OrderedContractExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(Map.of("ordered", extension));

        registry.dispatch(event(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", null));

        assertThat(extension.handled).containsExactly("first", "second");
        assertThat(registry.descriptors())
                .extracting(RuntimeEventHandlerDescriptor::order)
                .containsExactly(10, 20);
    }

    @Test
    void shouldMatchEntityAliasAndActionCodeWhenDeclared() {
        ActionExtension extension = new ActionExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(Map.of("action", extension));

        registry.dispatch(event(RuntimeEventType.ACTION_EXECUTED, "sales.contract", "contract", "submit"));
        registry.dispatch(event(RuntimeEventType.ACTION_EXECUTED, "sales.contract", "contract", "cancel"));
        registry.dispatch(event(RuntimeEventType.ACTION_EXECUTED, "sales.contract", "line", "submit"));

        assertThat(extension.count).isEqualTo(1);
    }

    @Test
    void handlerShouldUseModuleExtensionEntityAliasAsDefault() {
        DefaultEntityExtension extension = new DefaultEntityExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(Map.of("defaultEntity", extension));

        registry.dispatch(event(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", null));
        registry.dispatch(event(RuntimeEventType.AFTER_CREATE, "sales.contract", "line", null));

        assertThat(extension.count).isEqualTo(1);
    }

    @Test
    void afterLikeHandlersShouldDefaultToAfterCommitAndWarnFailure() {
        FailingAfterExtension extension = new FailingAfterExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(Map.of("failingAfter", extension));
        RuntimeEvent event = event(RuntimeEventType.AFTER_UPDATE, "sales.contract", "contract", null);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        registry.dispatch(event);

        assertThat(extension.count).isZero();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertThat(extension.count).isEqualTo(1);
    }

    @Test
    void blockingFailureShouldRaiseHandlerException() {
        BlockingFailureExtension extension = new BlockingFailureExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(Map.of("blocking", extension));

        assertThatThrownBy(() -> registry.dispatch(
                event(RuntimeEventType.ACTION_FAILED, "sales.contract", "contract", "submit")))
                .isInstanceOf(RuntimeEventHandlerException.class)
                .hasMessageContaining("runtime event handler failed");
    }

    @Test
    void shouldRejectInvalidHandlerSignature() throws Exception {
        InvalidExtension extension = new InvalidExtension();
        Method method = InvalidExtension.class.getDeclaredMethod("invalid", String.class);

        assertThatThrownBy(() -> RuntimeEventHandlerDescriptor.from("invalid", extension, method))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RuntimeEvent");
    }

    @Test
    void shouldRejectAfterCommitBlockingHandler() throws Exception {
        AfterCommitBlockingExtension extension = new AfterCommitBlockingExtension();
        Method method = AfterCommitBlockingExtension.class.getDeclaredMethod("handle", RuntimeEvent.class);

        assertThatThrownBy(() -> RuntimeEventHandlerDescriptor.from("afterCommitBlocking", extension, method))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AFTER_COMMIT")
                .hasMessageContaining("BLOCK");
    }

    @Test
    void shouldUseProvidedInspectionTypeForProxyLikeBeans() {
        ProxyLikeExtension proxyLikeBean = new ProxyLikeExtension();
        RuntimeEventHandlerRegistry registry = RuntimeEventHandlerRegistry.fromBeans(
                Map.of("proxy", proxyLikeBean),
                ignored -> ProxiedContractExtension.class,
                (bean, method) -> {
                    try {
                        return ProxyLikeExtension.class.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
        );

        registry.dispatch(event(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", null));

        assertThat(proxyLikeBean.count).isEqualTo(1);
    }

    private RuntimeEvent event(RuntimeEventType eventType, String moduleAlias, String entityAlias, String actionCode) {
        return RuntimeEvent.of(eventType, moduleAlias, entityAlias, "record-1", actionCode,
                "tenant-1", false, RuntimeMutationSource.BUSINESS, Map.of());
    }

    @ModuleExtension("sales.contract")
    static final class OrderedContractExtension {
        private final List<String> handled = new ArrayList<>();

        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE, entityAlias = "contract", order = 10)
        void first(RuntimeEvent event) {
            handled.add("first");
        }

        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE, entityAlias = "contract", order = 20)
        void second(RuntimeEvent event) {
            handled.add("second");
        }
    }

    @ModuleExtension(moduleAlias = "sales.contract")
    static final class ActionExtension {
        private int count;

        @RuntimeEventHandler(event = RuntimeEventType.ACTION_EXECUTED, entityAlias = "contract", actionCode = "submit")
        void handle(RuntimeEvent event) {
            count++;
        }
    }

    @ModuleExtension(moduleAlias = "sales.contract", entityAlias = "contract")
    static final class DefaultEntityExtension {
        private int count;

        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE)
        void handle(RuntimeEvent event) {
            count++;
        }
    }

    @ModuleExtension("sales.contract")
    static final class FailingAfterExtension {
        private int count;

        @RuntimeEventHandler(event = RuntimeEventType.AFTER_UPDATE)
        void handle(RuntimeEvent event) {
            count++;
            throw new IllegalStateException("ignored");
        }
    }

    @ModuleExtension("sales.contract")
    static final class BlockingFailureExtension {
        @RuntimeEventHandler(
                event = RuntimeEventType.ACTION_FAILED,
                actionCode = "submit",
                phase = RuntimeEventHandlerPhase.IN_TRANSACTION,
                failure = RuntimeEventHandlerFailurePolicy.BLOCK
        )
        void handle(RuntimeEvent event) {
            throw new IllegalStateException("blocked");
        }
    }

    @ModuleExtension("sales.contract")
    static final class InvalidExtension {
        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE)
        void invalid(String event) {
        }
    }

    @ModuleExtension("sales.contract")
    static final class AfterCommitBlockingExtension {
        @RuntimeEventHandler(
                event = RuntimeEventType.AFTER_UPDATE,
                phase = RuntimeEventHandlerPhase.AFTER_COMMIT,
                failure = RuntimeEventHandlerFailurePolicy.BLOCK
        )
        void handle(RuntimeEvent event) {
        }
    }

    @ModuleExtension("sales.contract")
    static class ProxiedContractExtension {
        @RuntimeEventHandler(event = RuntimeEventType.AFTER_CREATE, entityAlias = "contract")
        public void handle(RuntimeEvent event) {
        }
    }

    static final class ProxyLikeExtension {
        private int count;

        public void handle(RuntimeEvent event) {
            count++;
        }
    }
}
