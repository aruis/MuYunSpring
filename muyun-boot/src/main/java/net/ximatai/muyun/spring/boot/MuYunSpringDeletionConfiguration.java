package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import net.ximatai.muyun.spring.ability.deletion.DeletionTransactionOperator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Installs the platform deletion journal without coupling Ability to platform persistence. */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringDeletionConfiguration {
    @Bean
    DeletionLifecycleListenerRegistration deletionLifecycleListenerRegistration(
            ObjectProvider<DeletionLifecycleListener> listenerProvider) {
        return new DeletionLifecycleListenerRegistration(
                listenerProvider.getIfAvailable(() -> DeletionLifecycleListener.NONE));
    }

    @Bean
    DeletionTransactionRegistration deletionTransactionRegistration(
            ObjectProvider<PlatformTransactionManager> transactionManager) {
        PlatformTransactionManager manager = transactionManager.getIfAvailable();
        DeletionTransactionOperator operator = manager == null
                ? DeletionTransactionOperator.NONE
                : transactionOperator(new TransactionTemplate(manager));
        return new DeletionTransactionRegistration(operator);
    }

    static final class DeletionLifecycleListenerRegistration implements DisposableBean {
        DeletionLifecycleListenerRegistration(DeletionLifecycleListener listener) {
            PlatformAbilityRuntime.configureDeletionLifecycleListener(listener);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetDeletionLifecycleListener();
        }
    }

    private static DeletionTransactionOperator transactionOperator(TransactionTemplate transactionTemplate) {
        return new DeletionTransactionOperator() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> work) {
                return transactionTemplate.execute(status -> work.get());
            }
        };
    }

    static final class DeletionTransactionRegistration implements DisposableBean {
        DeletionTransactionRegistration(DeletionTransactionOperator operator) {
            PlatformAbilityRuntime.configureDeletionTransactionOperator(operator);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetDeletionTransactionOperator();
        }
    }
}
