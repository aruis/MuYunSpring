package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

import java.util.List;

@Getter
@Setter
final class DemoInvoice extends StandardEntity {
    private String title;
    @ReferenceTo(
            moduleAlias = "demo",
            entityCode = "customer",
            autoTitle = true,
            titleOutputField = "customerTitle",
            projections = @ReferenceProject(targetField = "status", outputField = "customerStatus")
    )
    private String customerId;
    private transient String customerTitle;
    private transient String customerStatus;
    @ChildRef(
            parentEntity = "invoice",
            childModel = DemoInvoiceLine.class,
            childEntity = "invoiceLine",
            childForeignKeyField = "invoiceId",
            autoPopulate = true,
            autoDeleteWithParent = true
    )
    private List<DemoInvoiceLine> lines;

    DemoInvoice(String title, List<DemoInvoiceLine> lines) {
        this.title = title;
        this.lines = lines;
    }
}
