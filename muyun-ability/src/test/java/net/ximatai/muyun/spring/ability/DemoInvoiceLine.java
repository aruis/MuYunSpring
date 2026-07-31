package net.ximatai.muyun.spring.ability;


import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;
import net.ximatai.muyun.spring.ability.child.ChildOf;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;

@Getter
@Setter
@SortPartitionBy(fields = "invoiceId")
final class DemoInvoiceLine extends StandardSortableEntity {
    @ChildOf
    @ReferenceTo(moduleAlias = "demo", entityAlias = "demoInvoice",
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.CASCADE_DELETE))
    private String invoiceId;

    DemoInvoiceLine(String title) {
        setTitle(title);
    }
}
