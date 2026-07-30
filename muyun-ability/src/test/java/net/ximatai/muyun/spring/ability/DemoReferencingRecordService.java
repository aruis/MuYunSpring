package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;

final class DemoReferencingRecordService implements
        CrudAbility<DemoReferencingRecord>,
        ReferencerAbility<DemoReferencingRecord> {
    private final InMemoryBaseDao<DemoReferencingRecord> dao = new InMemoryBaseDao<>();
    private final DemoCustomerService customerService = new DemoCustomerService();
    private final DemoUserService userService = new DemoUserService();

    DemoReferencingRecordService() {
        PlatformAbilityRuntime.configureReferenceTargetResolver(target -> {
            if (customerService.referenceTarget().equals(target)) {
                return java.util.Optional.of(customerService);
            }
            if (userService.referenceTarget().equals(target)) {
                return java.util.Optional.of(userService);
            }
            return java.util.Optional.empty();
        });
        DemoCustomer customer = new DemoCustomer("Customer One", "ACTIVE");
        customer.setId("customer-1");
        customerService.insert(customer);
        DemoUser owner = new DemoUser("Owner One");
        owner.setId("user-owner");
        userService.insert(owner);
        DemoUser firstWatcher = new DemoUser("Watcher One");
        firstWatcher.setId("user-watcher-1");
        userService.insert(firstWatcher);
        DemoUser secondWatcher = new DemoUser("Watcher Two");
        secondWatcher.setId("user-watcher-2");
        userService.insert(secondWatcher);
    }

    @Override
    public BaseDao<DemoReferencingRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.referencingRecord";
    }

}
