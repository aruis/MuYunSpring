package net.ximatai.muyun.spring.ability;


final class DemoEnabledRecordService extends AbstractAbilityService<DemoEnabledRecord> implements
        SoftDeleteAbility<DemoEnabledRecord>,
        EnableAbility<DemoEnabledRecord> {

    DemoEnabledRecordService() {
        super("demo.enabledRecord", DemoEnabledRecord.class, new InMemoryBaseDao<>());
    }
}
