package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolves statically declared resource services by their stable module alias. */
@Component
public class StaticDeletionRecoveryResourceResolver implements
        DeletionRecoveryResourceResolver,
        SmartInitializingSingleton {
    private final ListableBeanFactory beanFactory;
    private volatile Map<ResourceKey, SoftDeleteAbility<?>> abilities;

    @Autowired
    public StaticDeletionRecoveryResourceResolver(ListableBeanFactory beanFactory) {
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory must not be null");
        this.abilities = Map.of();
    }

    public StaticDeletionRecoveryResourceResolver(Collection<DeletionRecoveryAbility<?>> abilities) {
        this.beanFactory = null;
        this.abilities = indexAbilities(abilities);
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (beanFactory != null) {
            abilities = indexAbilities(recoveryAbilities(beanFactory));
        }
    }

    private static Map<ResourceKey, SoftDeleteAbility<?>> indexAbilities(
            Collection<DeletionRecoveryAbility<?>> abilities) {
        Map<ResourceKey, SoftDeleteAbility<?>> indexed = new LinkedHashMap<>();
        for (DeletionRecoveryAbility<?> ability
                : abilities == null ? List.<DeletionRecoveryAbility<?>>of() : abilities) {
            DeletionRecoveryAbility<?> identity = recoveryIdentity(ability);
            if (identity == null || blank(identity.getModuleAlias()) || blank(identity.getDeletionEntityAlias())) {
                continue;
            }
            ResourceKey key = new ResourceKey(identity.getModuleAlias(), identity.getDeletionEntityAlias());
            SoftDeleteAbility<?> previous = indexed.putIfAbsent(key, ability);
            if (previous != null && previous != ability) {
                throw new IllegalArgumentException("Duplicate static deletion recovery ability for " + key + ": "
                        + previous.getClass().getName() + ", "
                        + ability.getClass().getName());
            }
        }
        return Map.copyOf(indexed);
    }

    @Override
    public boolean supports(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        return abilities.containsKey(ResourceKey.from(entry));
    }

    @Override
    public Optional<SoftDeleteAbility<?>> resolve(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        return Optional.ofNullable(abilities.get(ResourceKey.from(entry)));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static DeletionRecoveryAbility<?> recoveryIdentity(DeletionRecoveryAbility<?> ability) {
        if (ability == null) {
            return null;
        }
        Object target = AopProxyUtils.getSingletonTarget(ability);
        return target instanceof DeletionRecoveryAbility<?> targetAbility ? targetAbility : ability;
    }

    @SuppressWarnings("rawtypes")
    private static Collection<DeletionRecoveryAbility<?>> recoveryAbilities(ListableBeanFactory beanFactory) {
        if (beanFactory == null) {
            return List.of();
        }
        List<DeletionRecoveryAbility<?>> abilities = new ArrayList<>();
        beanFactory.getBeansOfType(DeletionRecoveryAbility.class).values().forEach(abilities::add);
        return abilities;
    }

    private record ResourceKey(String moduleAlias, String entityAlias) {
        private static ResourceKey from(DeletionEntry entry) {
            return new ResourceKey(entry.getResourceModuleAlias(), entry.getResourceEntityAlias());
        }
    }
}
