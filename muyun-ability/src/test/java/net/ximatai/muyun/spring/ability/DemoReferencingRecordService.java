package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceLookup;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;


import java.util.List;

final class DemoReferencingRecordService implements
        CrudAbility<DemoReferencingRecord>,
        ReferencerAbility<DemoReferencingRecord> {
    private final InMemoryBaseDao<DemoReferencingRecord> dao = new InMemoryBaseDao<>();
    private final DemoCustomerService customerService = new DemoCustomerService();
    private final DemoUserService userService = new DemoUserService();

    DemoReferencingRecordService() {
        DemoCustomer customer = new DemoCustomer("Customer One", "ACTIVE");
        customer.setId("customer-1");
        customerService.insert(customer);
        DemoUser owner = new DemoUser("Owner One");
        owner.setId("user-owner");
        userService.insert(owner);
    }

    @Override
    public BaseDao<DemoReferencingRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.referencingRecord";
    }

    @Override
    public List<ReferenceLookup> referenceLookups() {
        return List.of(referenceLookup(customerService), referenceLookup(userService));
    }
}
