package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.child.StaticChildResolver;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class DemoInvoiceService extends AbstractAbilityService<DemoInvoice> implements
        SoftDeleteAbility<DemoInvoice>,
        ChildrenAbility<DemoInvoice>,
        ReferencerAbility<DemoInvoice>,
        CacheAbility<DemoInvoice> {
    private final DemoInvoiceLineService lineService = new DemoInvoiceLineService();
    private final DemoCustomerService customerService;
    private int businessHookCount;

    DemoInvoiceService() {
        this(new DemoCustomerService());
        DemoCustomer customer = new DemoCustomer("Customer One", "ACTIVE");
        customer.setId("customer-1");
        customerService.insert(customer);
    }

    DemoInvoiceService(DemoCustomerService customerService) {
        super("demo.invoice", new InMemoryBaseDao<>());
        this.customerService = customerService;
    }

    @Override
    public List<ChildRelation<? extends EntityContract, DemoInvoice>> childRelations() {
        return List.of(lineService
                .toChildRelation(
                        StaticChildResolver.plans(DemoInvoice.class).getFirst(),
                        DemoInvoiceLine::setInvoiceId,
                        DemoInvoice::getLines,
                        DemoInvoice::setLines
                ));
    }

    DemoInvoiceLineService lineService() {
        return lineService;
    }

    DemoCustomerService customerService() {
        return customerService;
    }

    @Override
    public DemoInvoice copyForCache(DemoInvoice entity) {
        if (entity == null) {
            return null;
        }
        DemoInvoice copy = new DemoInvoice(entity.getTitle(), null);
        copy.setCustomerId(entity.getCustomerId());
        copy.setCustomerTitle(entity.getCustomerTitle());
        copy.setCustomerStatus(entity.getCustomerStatus());
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
    public Class<?> referencingModelClass() {
        return DemoInvoice.class;
    }

    @Override
    public Map<String, String> referenceTitles(ReferenceTarget target, Collection<String> ids) {
        if (ReferenceTarget.of("demo", "customer").equals(target)) {
            return customerService.titles(ids);
        }
        return Map.of();
    }

    @Override
    public Map<String, Map<String, Object>> referenceProjections(ReferenceTarget target,
                                                                 Collection<String> ids,
                                                                 Collection<String> sourceFields) {
        if (ReferenceTarget.of("demo", "customer").equals(target)
                && sourceFields.contains("status")) {
            return customerService.projections(ids, sourceFields);
        }
        return Map.of();
    }

    @Override
    public void afterInsert(String id, DemoInvoice entity) {
        businessHookCount++;
    }

    @Override
    public void afterUpdate(DemoInvoice entity, int updated) {
        businessHookCount++;
    }

    @Override
    public void afterDelete(String id, DemoInvoice entity, int deleted) {
        businessHookCount++;
    }

    @Override
    public void afterSelect(DemoInvoice entity) {
        businessHookCount++;
    }

    int businessHookCount() {
        return businessHookCount;
    }
}
