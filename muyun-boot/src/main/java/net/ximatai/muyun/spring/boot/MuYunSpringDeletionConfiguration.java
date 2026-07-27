package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Installs the platform deletion journal without coupling Ability to platform persistence. */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringDeletionConfiguration {
    @Bean
    DeletionLifecycleListenerRegistration deletionLifecycleListenerRegistration(
            ObjectProvider<DeletionLifecycleListener> listenerProvider) {
        return new DeletionLifecycleListenerRegistration(
                listenerProvider.getIfAvailable(() -> DeletionLifecycleListener.NONE));
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
}
