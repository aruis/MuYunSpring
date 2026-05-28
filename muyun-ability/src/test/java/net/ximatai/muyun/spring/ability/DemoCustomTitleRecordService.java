package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoCustomTitleRecordService extends AbstractAbilityService<DemoCustomTitleRecord> implements
        ReferenceAbility<DemoCustomTitleRecord> {

    DemoCustomTitleRecordService() {
        super("demo.customTitleRecord", new InMemoryBaseDao<>());
    }
}
