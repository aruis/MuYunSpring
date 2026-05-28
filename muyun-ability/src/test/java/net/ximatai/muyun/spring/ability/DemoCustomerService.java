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
    public DemoCustomer copyForCache(DemoCustomer entity) {
        if (entity == null) {
            return null;
        }
        DemoCustomer copy = new DemoCustomer(entity.getTitle(), entity.getStatus());
        copy.setId(entity.getId());
        copy.setTenantId(entity.getTenantId());
        copy.setVersion(entity.getVersion());
        copy.setDeleted(entity.getDeleted());
        copy.setCreatedBy(entity.getCreatedBy());
        copy.setCreatedAt(entity.getCreatedAt());
        copy.setUpdatedBy(entity.getUpdatedBy());
        copy.setUpdatedAt(entity.getUpdatedAt());
        return copy;
    }

    @Override
    public void afterSelect(DemoCustomer entity) {
        afterSelectCount++;
    }

    int afterSelectCount() {
        return afterSelectCount;
    }
}
