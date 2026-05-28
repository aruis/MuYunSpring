package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;
import net.ximatai.muyun.spring.ability.child.ChildAbility;


final class DemoInvoiceLineService extends AbstractAbilityService<DemoInvoiceLine> implements
        SoftDeleteAbility<DemoInvoiceLine>,
        CascadeDeleteChildAbility<DemoInvoiceLine> {
    private int afterSelectCount;

    DemoInvoiceLineService() {
        super("demo.invoiceLine", new InMemoryBaseDao<>());
    }

    @Override
    public void afterSelect(DemoInvoiceLine entity) {
        afterSelectCount++;
    }

    int afterSelectCount() {
        return afterSelectCount;
    }
}
