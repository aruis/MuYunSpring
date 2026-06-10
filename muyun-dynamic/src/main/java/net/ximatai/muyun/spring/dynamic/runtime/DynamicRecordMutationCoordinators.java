package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DynamicRecordMutationCoordinators {
    private DynamicRecordMutationCoordinators() {
    }

    public static DynamicRecordMutationCoordinator composite(List<DynamicRecordMutationCoordinator> coordinators) {
        List<DynamicRecordMutationCoordinator> delegates = coordinators == null
                ? List.of()
                : coordinators.stream()
                .filter(Objects::nonNull)
                .filter(coordinator -> coordinator != DynamicRecordMutationCoordinator.NONE)
                .toList();
        if (delegates.isEmpty()) {
            return DynamicRecordMutationCoordinator.NONE;
        }
        if (delegates.size() == 1) {
            return delegates.getFirst();
        }
        return new Composite(delegates);
    }

    public static DynamicRecordMutationCoordinator lazyComposite(
            Supplier<List<DynamicRecordMutationCoordinator>> coordinators) {
        Objects.requireNonNull(coordinators, "coordinators must not be null");
        return new LazyComposite(coordinators);
    }

    private record Composite(List<DynamicRecordMutationCoordinator> delegates)
            implements DynamicRecordMutationCoordinator {
        private Composite {
            delegates = List.copyOf(delegates);
        }

        @Override
        public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
            delegates.forEach(delegate -> delegate.beforeCreate(moduleAlias, entityAlias, record));
        }

        @Override
        public void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
            delegates.forEach(delegate -> delegate.afterCreate(moduleAlias, entityAlias, record, id));
        }

        @Override
        public void beforeRelationChildCreate(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parent,
                                              DynamicRecord child) {
            delegates.forEach(delegate -> delegate.beforeRelationChildCreate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parent, child));
        }

        @Override
        public void afterRelationChildCreate(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parent,
                                             DynamicRecord child,
                                             String id) {
            delegates.forEach(delegate -> delegate.afterRelationChildCreate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parent, child, id));
        }

        @Override
        public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
            delegates.forEach(delegate -> delegate.beforeUpdate(moduleAlias, entityAlias, before, incoming));
        }

        @Override
        public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
            delegates.forEach(delegate -> delegate.afterUpdate(moduleAlias, entityAlias, before, updated));
        }

        @Override
        public void beforeRelationChildUpdate(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parentBefore,
                                              DynamicRecord parentIncoming,
                                              DynamicRecord childBefore,
                                              DynamicRecord childIncoming) {
            delegates.forEach(delegate -> delegate.beforeRelationChildUpdate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, parentIncoming, childBefore, childIncoming));
        }

        @Override
        public void afterRelationChildUpdate(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parentBefore,
                                             DynamicRecord parentUpdated,
                                             DynamicRecord childBefore,
                                             DynamicRecord childUpdated) {
            delegates.forEach(delegate -> delegate.afterRelationChildUpdate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, parentUpdated, childBefore, childUpdated));
        }

        @Override
        public void beforeRelationChildDelete(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parentBefore,
                                              DynamicRecord childBefore) {
            delegates.forEach(delegate -> delegate.beforeRelationChildDelete(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, childBefore));
        }

        @Override
        public void afterRelationChildDelete(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parentBefore,
                                             DynamicRecord childBefore) {
            delegates.forEach(delegate -> delegate.afterRelationChildDelete(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, childBefore));
        }

        @Override
        public void beforeDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            delegates.forEach(delegate -> delegate.beforeDelete(moduleAlias, entityAlias, before));
        }

        @Override
        public void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            delegates.forEach(delegate -> delegate.afterDelete(moduleAlias, entityAlias, before));
        }

        @Override
        public void afterMutation(DynamicRecordMutationEvent event) {
            delegates.forEach(delegate -> delegate.afterMutation(event));
        }
    }

    private record LazyComposite(Supplier<List<DynamicRecordMutationCoordinator>> coordinators)
            implements DynamicRecordMutationCoordinator {
        private List<DynamicRecordMutationCoordinator> delegates() {
            DynamicRecordMutationCoordinator coordinator = composite(coordinators.get());
            if (coordinator == DynamicRecordMutationCoordinator.NONE) {
                return List.of();
            }
            if (coordinator instanceof Composite composite) {
                return composite.delegates();
            }
            return List.of(coordinator);
        }

        @Override
        public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
            delegates().forEach(delegate -> delegate.beforeCreate(moduleAlias, entityAlias, record));
        }

        @Override
        public void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
            delegates().forEach(delegate -> delegate.afterCreate(moduleAlias, entityAlias, record, id));
        }

        @Override
        public void beforeRelationChildCreate(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parent,
                                              DynamicRecord child) {
            delegates().forEach(delegate -> delegate.beforeRelationChildCreate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parent, child));
        }

        @Override
        public void afterRelationChildCreate(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parent,
                                             DynamicRecord child,
                                             String id) {
            delegates().forEach(delegate -> delegate.afterRelationChildCreate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parent, child, id));
        }

        @Override
        public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
            delegates().forEach(delegate -> delegate.beforeUpdate(moduleAlias, entityAlias, before, incoming));
        }

        @Override
        public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
            delegates().forEach(delegate -> delegate.afterUpdate(moduleAlias, entityAlias, before, updated));
        }

        @Override
        public void beforeRelationChildUpdate(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parentBefore,
                                              DynamicRecord parentIncoming,
                                              DynamicRecord childBefore,
                                              DynamicRecord childIncoming) {
            delegates().forEach(delegate -> delegate.beforeRelationChildUpdate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, parentIncoming, childBefore, childIncoming));
        }

        @Override
        public void afterRelationChildUpdate(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parentBefore,
                                             DynamicRecord parentUpdated,
                                             DynamicRecord childBefore,
                                             DynamicRecord childUpdated) {
            delegates().forEach(delegate -> delegate.afterRelationChildUpdate(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, parentUpdated, childBefore, childUpdated));
        }

        @Override
        public void beforeRelationChildDelete(String moduleAlias,
                                              String parentEntityAlias,
                                              String relationCode,
                                              String childEntityAlias,
                                              DynamicRecord parentBefore,
                                              DynamicRecord childBefore) {
            delegates().forEach(delegate -> delegate.beforeRelationChildDelete(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, childBefore));
        }

        @Override
        public void afterRelationChildDelete(String moduleAlias,
                                             String parentEntityAlias,
                                             String relationCode,
                                             String childEntityAlias,
                                             DynamicRecord parentBefore,
                                             DynamicRecord childBefore) {
            delegates().forEach(delegate -> delegate.afterRelationChildDelete(moduleAlias, parentEntityAlias,
                    relationCode, childEntityAlias, parentBefore, childBefore));
        }

        @Override
        public void beforeDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            delegates().forEach(delegate -> delegate.beforeDelete(moduleAlias, entityAlias, before));
        }

        @Override
        public void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
            delegates().forEach(delegate -> delegate.afterDelete(moduleAlias, entityAlias, before));
        }

        @Override
        public void afterMutation(DynamicRecordMutationEvent event) {
            delegates().forEach(delegate -> delegate.afterMutation(event));
        }
    }
}
