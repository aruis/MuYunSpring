package net.ximatai.muyun.spring.ability;


import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;

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
