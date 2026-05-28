package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;
import net.ximatai.muyun.spring.ability.child.ChildAbility;


final class DemoInvoiceLineService implements
        CrudAbility<DemoInvoiceLine>,
        SoftDeleteAbility<DemoInvoiceLine>,
        CascadeDeleteChildAbility<DemoInvoiceLine> {
    private final InMemoryBaseDao<DemoInvoiceLine> dao = new InMemoryBaseDao<>();
    private int afterSelectCount;

    @Override
    public BaseDao<DemoInvoiceLine, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.invoiceLine";
    }

    @Override
    public void afterSelect(DemoInvoiceLine entity) {
        afterSelectCount++;
    }

    int afterSelectCount() {
        return afterSelectCount;
    }
}
