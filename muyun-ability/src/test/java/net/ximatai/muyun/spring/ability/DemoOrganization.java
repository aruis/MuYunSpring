package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;
import net.ximatai.muyun.spring.common.model.TitledCapable;
import net.ximatai.muyun.spring.common.model.TreeCapable;

@Getter
@Setter
final class DemoOrganization extends StandardEntity implements TreeCapable, TitledCapable {
    private String parentId;
    private String title;
    private Integer sortOrder;

    DemoOrganization(String title, String parentId) {
        this.title = title;
        this.parentId = parentId;
    }
}
