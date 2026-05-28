package net.ximatai.muyun.spring.ability;


import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;

@Getter
@Setter
final class DemoOrganization extends StandardEntity implements TreeCapable, TitledCapable {
    private String parentId;
    @TitleField
    private String title;
    private Integer sortOrder;

    DemoOrganization(String title, String parentId) {
        this.title = title;
        this.parentId = parentId;
    }
}
