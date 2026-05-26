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
    public Map<String, Set<String>> collectReferenceIdsBySourceNamespace(DemoReferencingRecord entity) {
        Map<String, Set<String>> ids = new LinkedHashMap<>();
        if (entity.getCustomerId() != null) {
            ids.put("demo.customer", Set.of(entity.getCustomerId()));
        }
        LinkedHashSet<String> users = new LinkedHashSet<>();
        if (entity.getCreatedBy() != null) {
            users.add(entity.getCreatedBy());
        }
        if (entity.getOwnerId() != null) {
            users.add(entity.getOwnerId());
        }
        if (!users.isEmpty()) {
            ids.put("iam.user", users);
        }
        return ids;
    }
}
