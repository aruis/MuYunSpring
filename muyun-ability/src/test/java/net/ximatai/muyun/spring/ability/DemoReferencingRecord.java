package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.StandardEntity;

@Getter
@Setter
final class DemoReferencingRecord extends StandardEntity {
    @ReferenceTo(moduleAlias = "demo", entityCode = "customer")
    private String customerId;
    @ReferenceTo(moduleAlias = "iam", entityCode = "user")
    private String ownerId;
    @ReferenceTo(moduleAlias = "iam", entityCode = "user", cardinality = ReferenceCardinality.MANY)
    private String watcherIds;

    DemoReferencingRecord(String customerId, String ownerId) {
        this.customerId = customerId;
        this.ownerId = ownerId;
    }
}
