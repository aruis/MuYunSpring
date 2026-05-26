package net.ximatai.muyun.spring.ability;

import java.util.List;

final class DemoInvoiceService implements
        CrudAbility<DemoInvoice>,
        SoftDeleteAbility<DemoInvoice>,
        ChildrenAbility<DemoInvoice> {
    private final InMemoryBaseDao<DemoInvoice> dao = new InMemoryBaseDao<>();
    private final DemoInvoiceLineService lineService = new DemoInvoiceLineService();

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
}
