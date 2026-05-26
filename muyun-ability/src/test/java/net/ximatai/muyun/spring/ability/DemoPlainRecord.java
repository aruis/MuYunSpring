package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

@Getter
@Setter
final class DemoPlainRecord extends StandardEntity {
    private String title;

    DemoPlainRecord(String title) {
        this.title = title;
    }
}
