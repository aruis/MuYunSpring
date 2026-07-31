package net.ximatai.muyun.spring.platform.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoadPath;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoadReader;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoadResolver;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.StaticReferenceResolver;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

/** Executes compiled reference-load paths through the common reference projection contract. */
public final class PlatformReferenceLoadResolver implements ReferenceLoadResolver {
    private final StaticAbilityCatalog abilities;

    public PlatformReferenceLoadResolver(StaticAbilityCatalog abilities) {
        this.abilities = abilities;
        validatePaths();
    }

    @Override
    public void populate(CrudAbility<?> ability, EntityContract entity) {
        if (ability == null || entity == null) {
            return;
        }
        Class<?> modelClass = ability.modelClass() == null ? entity.getClass() : ability.modelClass();
        for (ReferenceLoadPath path : StaticReferenceResolver.loadPaths(modelClass)) {
            populatePath(entity, path);
        }
    }

    private void populatePath(EntityContract entity, ReferenceLoadPath path) {
        List<String> currentIds = StaticReferenceResolver.values(entity, sourcePlan(entity.getClass(), path.sourceField()));
        if (currentIds.isEmpty()) {
            StaticReferenceResolver.writeLoadedValue(entity, path.outputField(), null);
            return;
        }
        StaticReferenceResolver.writeLoadedValue(entity, path.outputField(),
                ReferenceLoadReader.read(resolvePath(path), currentIds, this::requireReferenceAbility));
    }

    private void validatePaths() {
        for (CrudAbility<?> ability : abilities.abilities()) {
            Class<?> modelClass = ability.modelClass();
            if (modelClass == null) {
                continue;
            }
            for (ReferenceLoadPath path : StaticReferenceResolver.loadPaths(modelClass)) {
                ReferenceTarget currentTarget = path.sourceTarget();
                for (ReferenceLoadPath.Hop hop : path.hops()) {
                    CrudAbility<?> currentAbility = requireAbility(currentTarget, "hop");
                    viaRule(currentAbility, hop);
                    currentTarget = hop.target();
                }
                ReferenceAbility<?> terminalAbility = requireReferenceAbility(currentTarget);
                StaticReferenceResolver.requireReadableField(terminalAbility.modelClass(), path.terminalField(),
                        "ReferenceLoad terminal");
            }
        }
    }

    private StaticReferenceResolver.ReferenceRule viaRule(CrudAbility<?> currentAbility, ReferenceLoadPath.Hop hop) {
        List<StaticReferenceResolver.ReferenceRule> matches = StaticReferenceResolver.rules(currentAbility.modelClass()).stream()
                .filter(rule -> rule.cardinality() == ReferenceCardinality.ONE)
                .filter(rule -> hop.target().equals(rule.target()))
                .filter(rule -> hop.viaField() == null || hop.viaField().equals(rule.plan().sourceField()))
                .toList();
        if (matches.size() == 1) {
            return matches.getFirst();
        }
        String qualifier = hop.viaField() == null ? "" : ", via=" + hop.viaField();
        throw new PlatformException("ReferenceLoad requires exactly one reference hop: "
                + currentAbility.getModuleAlias() + " -> " + hop.target().qualifiedName() + qualifier);
    }

    private ReferenceLoadPath resolvePath(ReferenceLoadPath path) {
        ReferenceTarget currentTarget = path.sourceTarget();
        List<ReferenceLoadPath.Hop> resolved = new java.util.ArrayList<>();
        for (ReferenceLoadPath.Hop hop : path.hops()) {
            StaticReferenceResolver.ReferenceRule viaRule = viaRule(requireAbility(currentTarget, "hop"), hop);
            resolved.add(new ReferenceLoadPath.Hop(hop.target(), viaRule.plan().sourceField()));
            currentTarget = hop.target();
        }
        return new ReferenceLoadPath(path.sourceField(), path.sourceTarget(), resolved,
                path.terminalField(), path.outputField());
    }

    private net.ximatai.muyun.spring.ability.reference.ReferencePlan sourcePlan(Class<?> modelClass, String sourceField) {
        return StaticReferenceResolver.rules(modelClass).stream()
                .filter(rule -> sourceField.equals(rule.plan().sourceField()))
                .findFirst()
                .map(StaticReferenceResolver.ReferenceRule::plan)
                .orElseThrow(() -> new PlatformException("ReferenceLoad source is unavailable: "
                        + modelClass.getName() + "." + sourceField));
    }

    private CrudAbility<?> requireAbility(ReferenceTarget target, String role) {
        return abilities.findByTarget(target).orElseThrow(() -> new PlatformException(
                "ReferenceLoad " + role + " service is not registered: " + target.qualifiedName()));
    }

    private ReferenceAbility<?> requireReferenceAbility(ReferenceTarget target) {
        CrudAbility<?> ability = requireAbility(target, "terminal");
        if (ability instanceof ReferenceAbility<?> referenceAbility) {
            return referenceAbility;
        }
        throw new PlatformException("ReferenceLoad terminal service must implement ReferenceAbility: "
                + target.qualifiedName());
    }

}
