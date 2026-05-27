package net.ximatai.muyun.spring.ability;

final class DemoCustomerService implements
        CrudAbility<DemoCustomer>,
        ReferenceAbility<DemoCustomer>,
        CacheAbility<DemoCustomer> {
    private final InMemoryBaseDao<DemoCustomer> dao = new InMemoryBaseDao<>();
    private int afterSelectCount;

    @Override
    public BaseDao<DemoCustomer, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.customer";
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
