package net.ximatai.muyun.spring.ability;


import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
final class DemoInvoiceLine extends StandardSortableEntity {
    private String invoiceId;

    DemoInvoiceLine(String title) {
        setTitle(title);
    }
}
