package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoCustomerService extends AbstractAbilityService<DemoCustomer> implements
        ReferenceAbility<DemoCustomer>,
        CacheAbility<DemoCustomer> {
    private int afterSelectCount;

    DemoCustomerService() {
        super("demo.customer", new InMemoryBaseDao<>());
    }

    @Override
    public void afterSelect(DemoCustomer entity) {
        afterSelectCount++;
    }

    int afterSelectCount() {
        return afterSelectCount;
    }
}
