package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.CompositeReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;
import net.ximatai.muyun.spring.ability.reference.ReferencedByResolver;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoadResolver;
import net.ximatai.muyun.spring.ability.child.ChildAbilityResolver;
import net.ximatai.muyun.spring.platform.reference.DynamicReferenceDeletionGuard;
import net.ximatai.muyun.spring.platform.reference.PlatformReferenceTargetResolver;
import net.ximatai.muyun.spring.platform.reference.PlatformReferencedByResolver;
import net.ximatai.muyun.spring.platform.reference.PlatformReferenceLoadResolver;
import net.ximatai.muyun.spring.platform.reference.StaticReferenceDeletionGuard;
import net.ximatai.muyun.spring.platform.reference.StaticAbilityCatalog;
import net.ximatai.muyun.spring.platform.reference.PlatformChildAbilityResolver;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringReferenceConfiguration {
    @Bean
    StaticAbilityCatalog staticAbilityCatalog(List<CrudAbility<?>> abilities) {
        return new StaticAbilityCatalog(abilities);
    }

    @Bean
    ReferenceDeletionGuard staticReferenceDeletionGuard(StaticAbilityCatalog abilities) {
        return new StaticReferenceDeletionGuard(abilities.abilities());
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
            StaticAbilityCatalog staticAbilities,
            ObjectProvider<DynamicRecordRuntime> dynamicRuntime) {
        return new ReferenceTargetResolverRegistration(new PlatformReferenceTargetResolver(
                staticAbilities, dynamicRuntime.getIfAvailable()));
    }

    @Bean
    ReferencedByResolverRegistration referencedByResolverRegistration(StaticAbilityCatalog abilities) {
        return new ReferencedByResolverRegistration(new PlatformReferencedByResolver(abilities));
    }

    @Bean
    ReferenceLoadResolverRegistration referenceLoadResolverRegistration(StaticAbilityCatalog abilities) {
        return new ReferenceLoadResolverRegistration(new PlatformReferenceLoadResolver(abilities));
    }

    @Bean
    ChildAbilityResolverRegistration childAbilityResolverRegistration(StaticAbilityCatalog abilities) {
        return new ChildAbilityResolverRegistration(new PlatformChildAbilityResolver(abilities));
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

    static final class ReferencedByResolverRegistration implements DisposableBean {
        ReferencedByResolverRegistration(ReferencedByResolver resolver) {
            PlatformAbilityRuntime.configureReferencedByResolver(resolver);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetReferencedByResolver();
        }
    }

    static final class ReferenceLoadResolverRegistration implements DisposableBean {
        ReferenceLoadResolverRegistration(ReferenceLoadResolver resolver) {
            PlatformAbilityRuntime.configureReferenceLoadResolver(resolver);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetReferenceLoadResolver();
        }
    }

    static final class ChildAbilityResolverRegistration implements DisposableBean {
        ChildAbilityResolverRegistration(ChildAbilityResolver resolver) {
            PlatformAbilityRuntime.configureChildAbilityResolver(resolver);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetChildAbilityResolver();
        }
    }
}
