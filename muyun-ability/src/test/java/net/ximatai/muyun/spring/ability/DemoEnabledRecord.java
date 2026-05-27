package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.EnabledCapable;
import net.ximatai.muyun.spring.common.model.StandardEntity;

@Getter
@Setter
final class DemoEnabledRecord extends StandardEntity implements EnabledCapable {
    private String title;
    private Boolean enabled;

    DemoEnabledRecord(String title) {
        this.title = title;
    }
}
