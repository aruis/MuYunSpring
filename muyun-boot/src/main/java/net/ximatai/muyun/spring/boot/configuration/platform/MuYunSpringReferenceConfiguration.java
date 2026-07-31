package net.ximatai.muyun.spring.boot.configuration.platform;

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

/**
 * 引用与父子能力装配：从静态 Ability 目录和可选动态运行时编译解析器，
 * 再以可复位注册方式安装到统一 Ability 链路。
 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringReferenceConfiguration {
    @Bean
    /** 收集静态 CRUD Ability，作为引用、反向引用和子表解析的共同事实来源。 */
    StaticAbilityCatalog staticAbilityCatalog(List<CrudAbility<?>> abilities) {
        return new StaticAbilityCatalog(abilities);
    }

    @Bean
    /** 根据静态引用完整性声明构造删除保护器。 */
    ReferenceDeletionGuard staticReferenceDeletionGuard(StaticAbilityCatalog abilities) {
        return new StaticReferenceDeletionGuard(abilities.abilities());
    }

    @Bean
    /** 动态运行时存在时追加动态引用删除保护；否则显式为空实现。 */
    ReferenceDeletionGuard dynamicReferenceDeletionGuard(ObjectProvider<DynamicRecordRuntime> runtime) {
        DynamicRecordRuntime value = runtime.getIfAvailable();
        return value == null ? ReferenceDeletionGuard.NONE : new DynamicReferenceDeletionGuard(value);
    }

    @Bean
    /** 合并静态与动态删除保护，并注册到统一删除前置校验链。 */
    ReferenceDeletionGuardRegistration referenceDeletionGuardRegistration(
            ObjectProvider<ReferenceDeletionGuard> guardProvider) {
        return new ReferenceDeletionGuardRegistration(
                new CompositeReferenceDeletionGuard(guardProvider.orderedStream().toList()));
    }

    @Bean
    /** 将静态及动态目标解析规则安装到引用候选与标题投影链路。 */
    ReferenceTargetResolverRegistration referenceTargetResolverRegistration(
            StaticAbilityCatalog staticAbilities,
            ObjectProvider<DynamicRecordRuntime> dynamicRuntime) {
        return new ReferenceTargetResolverRegistration(new PlatformReferenceTargetResolver(
                staticAbilities, dynamicRuntime.getIfAvailable()));
    }

    @Bean
    /** 注册反向引用解析器，为 {@code @ReferencedBy} 提供运行时装配能力。 */
    ReferencedByResolverRegistration referencedByResolverRegistration(StaticAbilityCatalog abilities) {
        return new ReferencedByResolverRegistration(new PlatformReferencedByResolver(abilities));
    }

    @Bean
    /** 注册多跳引用字段加载解析器，静态与动态路径共享其投影语义。 */
    ReferenceLoadResolverRegistration referenceLoadResolverRegistration(StaticAbilityCatalog abilities) {
        return new ReferenceLoadResolverRegistration(new PlatformReferenceLoadResolver(abilities));
    }

    @Bean
    /** 注册子表 Ability 解析器，使父子聚合不依赖 Service 手工连接。 */
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
