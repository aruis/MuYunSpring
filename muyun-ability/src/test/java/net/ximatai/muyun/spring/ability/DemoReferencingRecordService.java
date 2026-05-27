package net.ximatai.muyun.spring.ability;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DemoReferencingRecordService implements
        CrudAbility<DemoReferencingRecord>,
        ReferencerAbility<DemoReferencingRecord> {
    private final InMemoryBaseDao<DemoReferencingRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoReferencingRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.referencingRecord";
    }

    @Override
    public Map<ReferenceTarget, Set<String>> collectReferenceIdsByTarget(DemoReferencingRecord entity) {
        Map<ReferenceTarget, Set<String>> ids = new LinkedHashMap<>();
        if (entity.getCustomerId() != null) {
            ids.put(ReferenceTarget.of("demo", "customer"), Set.of(entity.getCustomerId()));
        }
        LinkedHashSet<String> users = new LinkedHashSet<>();
        if (entity.getCreatedBy() != null) {
            users.add(entity.getCreatedBy());
        }
        if (entity.getOwnerId() != null) {
            users.add(entity.getOwnerId());
        }
        if (!users.isEmpty()) {
            ids.put(ReferenceTarget.of("iam", "user"), users);
        }
        return ids;
    }
}
