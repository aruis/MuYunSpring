package net.ximatai.muyun.spring.ability;


import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;

@Getter
@Setter
final class DemoPlainRecord extends StandardEntity {
    @TitleField
    private String title;

    DemoPlainRecord(String title) {
        this.title = title;
    }
}
