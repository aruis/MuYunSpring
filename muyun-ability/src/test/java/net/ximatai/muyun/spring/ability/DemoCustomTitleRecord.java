package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;
import net.ximatai.muyun.spring.common.model.TitleField;
import net.ximatai.muyun.spring.common.model.TitledCapable;

@Getter
@Setter
final class DemoCustomTitleRecord extends StandardEntity implements TitledCapable {
    private String title;
    @TitleField
    private String displayName;

    DemoCustomTitleRecord(String title, String displayName) {
        this.title = title;
        this.displayName = displayName;
    }
}
