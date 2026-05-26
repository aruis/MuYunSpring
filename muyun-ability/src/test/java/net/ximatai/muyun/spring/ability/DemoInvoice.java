package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

import java.util.List;

@Getter
@Setter
final class DemoInvoice extends StandardEntity {
    private String title;
    private List<DemoInvoiceLine> lines;

    DemoInvoice(String title, List<DemoInvoiceLine> lines) {
        this.title = title;
        this.lines = lines;
    }
}
