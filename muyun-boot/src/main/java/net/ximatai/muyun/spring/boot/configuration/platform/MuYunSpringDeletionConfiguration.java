package net.ximatai.muyun.spring.boot.configuration.platform;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import net.ximatai.muyun.spring.ability.deletion.DeletionTransactionOperator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 删除链路装配：把平台删除日志和事务执行器安装到 Ability 运行时，
 * 使领域 Service 无需感知日志持久化或 Spring 事务 API。
 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringDeletionConfiguration {
    @Bean
    /** 注入删除生命周期监听器；应用未提供时使用显式空实现。 */
    DeletionLifecycleListenerRegistration deletionLifecycleListenerRegistration(
            ObjectProvider<DeletionLifecycleListener> listenerProvider) {
        return new DeletionLifecycleListenerRegistration(
                listenerProvider.getIfAvailable(() -> DeletionLifecycleListener.NONE));
    }

    @Bean
    /** 有事务管理器时让删除前后动作共享事务；无事务宿主保持可用。 */
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
