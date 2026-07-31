package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.ability.child.ChildOf;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;

@Getter
@Setter
final class DemoInvoiceNote extends StandardEntity {
    @ChildOf
    @ReferenceTo(moduleAlias = "demo", entityAlias = "demoInvoice",
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.CASCADE_DELETE))
    private String invoiceId;
    private String content;

    DemoInvoiceNote() {
    }

    DemoInvoiceNote(String content) {
        this.content = content;
    }
}
