package net.ximatai.muyun.spring.ability;

import java.util.List;

final class DemoInvoiceService implements
        CrudAbility<DemoInvoice>,
        SoftDeleteAbility<DemoInvoice>,
        ChildrenAbility<DemoInvoice> {
    private final InMemoryBaseDao<DemoInvoice> dao = new InMemoryBaseDao<>();
    private final DemoInvoiceLineService lineService = new DemoInvoiceLineService();
    private int businessHookCount;

    @Override
    public BaseDao<DemoInvoice, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.invoice";
    }

    @Override
    public List<ChildRelation<? extends net.ximatai.muyun.spring.common.model.EntityContract, DemoInvoice>> childRelations() {
        return List.of(lineService
                .toChildRelation(DemoInvoiceLine::setInvoiceId, "invoiceId", DemoInvoice::getLines)
                .autoPopulate(DemoInvoice::setLines)
                .autoDeleteWithParent());
    }

    DemoInvoiceLineService lineService() {
        return lineService;
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
