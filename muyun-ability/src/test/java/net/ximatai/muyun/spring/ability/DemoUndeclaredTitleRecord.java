package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;
import net.ximatai.muyun.spring.common.model.TitledCapable;

@Getter
@Setter
final class DemoUndeclaredTitleRecord extends StandardEntity implements TitledCapable {
    private String title;

    DemoUndeclaredTitleRecord(String title) {
        this.title = title;
    }
}
