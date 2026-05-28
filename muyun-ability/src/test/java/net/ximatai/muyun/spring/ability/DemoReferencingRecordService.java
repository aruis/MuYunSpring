package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;


import java.util.Collection;
import java.util.Map;

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
    public Class<?> referencingModelClass() {
        return DemoReferencingRecord.class;
    }

    @Override
    public Map<String, String> referenceTitles(ReferenceTarget target, Collection<String> ids) {
        if (ReferenceTarget.of("demo", "customer").equals(target) && ids.contains("customer-1")) {
            return Map.of("customer-1", "Customer One");
        }
        return Map.of();
    }

    @Override
    public Map<String, Map<String, Object>> referenceProjections(ReferenceTarget target,
                                                                 Collection<String> ids,
                                                                 Collection<String> sourceFields) {
        if (ReferenceTarget.of("demo", "customer").equals(target)
                && ids.contains("customer-1")
                && sourceFields.contains("status")) {
            return Map.of("customer-1", Map.of("status", "ACTIVE"));
        }
        return Map.of();
    }
}
