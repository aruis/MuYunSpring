package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
final class DemoInvoiceNote extends StandardEntity {
    private String invoiceId;
    private String content;

    DemoInvoiceNote() {
    }

    DemoInvoiceNote(String content) {
        this.content = content;
    }
}
