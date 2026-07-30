package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.ChildOf;
import net.ximatai.muyun.spring.ability.child.Children;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;

import java.util.List;

@Getter
@Setter
final class DemoInvoice extends StandardEntity implements TitledCapable {
    private String title;
    @ReferenceTo(moduleAlias = "demo", entityAlias = "customer")
    private String customerId;
    @ReferenceLoad(source = "customerId", field = "title")
    private transient String customerTitle;
    @ReferenceLoad(source = "customerId", field = "status")
    private transient String customerStatus;
    @Children
    private List<DemoInvoiceLine> lines;
    @Children
    private List<DemoInvoiceNote> notes;

    DemoInvoice() {
    }

    DemoInvoice(String title, List<DemoInvoiceLine> lines) {
        this.title = title;
        this.lines = lines;
    }
}
