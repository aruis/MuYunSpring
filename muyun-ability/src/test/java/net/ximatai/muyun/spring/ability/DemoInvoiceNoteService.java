package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;

final class DemoInvoiceNoteService extends AbstractAbilityService<DemoInvoiceNote> implements
        SoftDeleteAbility<DemoInvoiceNote>,
        CascadeDeleteChildAbility<DemoInvoiceNote> {
    DemoInvoiceNoteService() {
        super("demo.invoiceNote", DemoInvoiceNote.class, new InMemoryBaseDao<>());
    }
}
