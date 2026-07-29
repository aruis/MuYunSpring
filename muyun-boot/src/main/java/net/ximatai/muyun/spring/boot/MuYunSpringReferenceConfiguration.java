package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.CompositeReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;
import net.ximatai.muyun.spring.boot.reference.DynamicReferenceDeletionGuard;
import net.ximatai.muyun.spring.boot.reference.PlatformReferenceTargetResolver;
import net.ximatai.muyun.spring.boot.reference.StaticReferenceDeletionGuard;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringReferenceConfiguration {
    @Bean
    ReferenceDeletionGuard staticReferenceDeletionGuard(ObjectProvider<CrudAbility<?>> abilities) {
        return new StaticReferenceDeletionGuard(abilities.orderedStream().toList());
    }

    @Bean
    ReferenceDeletionGuard dynamicReferenceDeletionGuard(ObjectProvider<DynamicRecordRuntime> runtime) {
        DynamicRecordRuntime value = runtime.getIfAvailable();
        return value == null ? ReferenceDeletionGuard.NONE : new DynamicReferenceDeletionGuard(value);
    }

    @Bean
    ReferenceDeletionGuardRegistration referenceDeletionGuardRegistration(
            ObjectProvider<ReferenceDeletionGuard> guardProvider) {
        return new ReferenceDeletionGuardRegistration(
                new CompositeReferenceDeletionGuard(guardProvider.orderedStream().toList()));
    }

    @Bean
    ReferenceTargetResolverRegistration referenceTargetResolverRegistration(
            ObjectProvider<ReferenceAbility<?>> staticAbilities,
            ObjectProvider<DynamicRecordRuntime> dynamicRuntime) {
        return new ReferenceTargetResolverRegistration(new PlatformReferenceTargetResolver(
                staticAbilities.orderedStream().toList(), dynamicRuntime.getIfAvailable()));
    }

    static final class ReferenceDeletionGuardRegistration implements DisposableBean {
        ReferenceDeletionGuardRegistration(ReferenceDeletionGuard guard) {
            PlatformAbilityRuntime.configureReferenceDeletionGuard(guard);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetReferenceDeletionGuard();
        }
    }

    static final class ReferenceTargetResolverRegistration implements DisposableBean {
        ReferenceTargetResolverRegistration(ReferenceTargetResolver resolver) {
            PlatformAbilityRuntime.configureReferenceTargetResolver(resolver);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetReferenceTargetResolver();
        }
    }
}
