package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;

@Getter
@Setter
final class DemoUser extends StandardEntity implements TitledCapable {
    @TitleField
    private String title;

    DemoUser() {
    }

    DemoUser(String title) {
        this.title = title;
    }
}
