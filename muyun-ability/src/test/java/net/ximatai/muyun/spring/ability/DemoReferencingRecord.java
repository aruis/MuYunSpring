package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

@Getter
@Setter
final class DemoReferencingRecord extends StandardEntity {
    private String customerId;
    private String ownerId;

    DemoReferencingRecord(String customerId, String ownerId) {
        this.customerId = customerId;
        this.ownerId = ownerId;
    }
}
