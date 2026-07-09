package net.ximatai.muyun.spring.ability.reference;

import java.util.ArrayList;
import java.util.List;

public record ModuleReferencePath(List<Step> steps, ModuleFieldRef targetField) {
    public ModuleReferencePath {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("module reference path steps must not be empty");
        }
        steps = List.copyOf(steps);
        if (targetField == null) {
            throw new IllegalArgumentException("module reference path target field must not be null");
        }
    }

    public static <T, R> Builder from(ModuleProperty<T, R> referenceField) {
        return new Builder(List.of(new Step(Direction.DIRECT, ModuleFieldRef.of(referenceField))));
    }

    public static <T, R> Builder inverse(ModuleProperty<T, R> bridgeToCurrentField) {
        return new Builder(List.of(new Step(Direction.INVERSE, ModuleFieldRef.of(bridgeToCurrentField))));
    }

    public enum Direction {
        DIRECT,
        INVERSE
    }

    public record Step(Direction direction, ModuleFieldRef referenceField) {
        public Step {
            if (direction == null) {
                throw new IllegalArgumentException("module reference path step direction must not be null");
            }
            if (referenceField == null) {
                throw new IllegalArgumentException("module reference path step field must not be null");
            }
        }
    }

    public static final class Builder {
        private final List<Step> steps;

        private Builder(List<Step> steps) {
            this.steps = new ArrayList<>(steps);
        }

        public <T, R> Builder then(ModuleProperty<T, R> referenceField) {
            steps.add(new Step(Direction.DIRECT, ModuleFieldRef.of(referenceField)));
            return this;
        }

        public <T, R> Builder thenInverse(ModuleProperty<T, R> bridgeToCurrentField) {
            steps.add(new Step(Direction.INVERSE, ModuleFieldRef.of(bridgeToCurrentField)));
            return this;
        }

        public <T, R> ModuleReferencePath select(ModuleProperty<T, R> targetField) {
            return new ModuleReferencePath(steps, ModuleFieldRef.of(targetField));
        }
    }
}
