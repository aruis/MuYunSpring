package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

@Getter
@Setter
final class DemoInvoiceLine extends StandardEntity {
    private String invoiceId;
    private String title;

    DemoInvoiceLine(String title) {
        this.title = title;
    }
}
