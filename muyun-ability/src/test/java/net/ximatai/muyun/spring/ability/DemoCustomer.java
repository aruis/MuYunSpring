package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;
import net.ximatai.muyun.spring.common.model.TitleField;
import net.ximatai.muyun.spring.common.model.TitledCapable;

@Getter
@Setter
final class DemoCustomer extends StandardEntity implements TitledCapable {
    @TitleField
    private String title;
    private String status;

    DemoCustomer(String title, String status) {
        this.title = title;
        this.status = status;
    }
}
