package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoCustomTitleRecordService implements
        CrudAbility<DemoCustomTitleRecord>,
        ReferenceAbility<DemoCustomTitleRecord> {
    private final InMemoryBaseDao<DemoCustomTitleRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoCustomTitleRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.customTitleRecord";
    }
}
