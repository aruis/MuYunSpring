package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoUndeclaredTitleRecordService extends AbstractAbilityService<DemoUndeclaredTitleRecord> implements
        ReferenceAbility<DemoUndeclaredTitleRecord> {

    DemoUndeclaredTitleRecordService() {
        super("demo.undeclaredTitleRecord", DemoUndeclaredTitleRecord.class, new InMemoryBaseDao<>());
    }
}
