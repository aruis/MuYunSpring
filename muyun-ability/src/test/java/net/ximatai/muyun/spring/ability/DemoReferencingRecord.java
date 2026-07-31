package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
final class DemoReferencingRecord extends StandardEntity {
    @ReferenceTo(moduleAlias = "demo", entityAlias = "customer")
    private String customerId;
    @ReferenceLoad(source = "customerId", field = "title")
    private transient String customerTitle;
    @ReferenceLoad(source = "customerId", field = "status")
    private transient String customerStatus;
    @ReferenceTo(moduleAlias = "iam", entityAlias = "user")
    private String ownerId;
    @ReferenceLoad(source = "ownerId", field = "title")
    private transient String ownerTitle;
    @ReferenceTo(moduleAlias = "iam", entityAlias = "user", cardinality = ReferenceCardinality.MANY)
    private String watcherIds;

    DemoReferencingRecord(String customerId, String ownerId) {
        this.customerId = customerId;
        this.ownerId = ownerId;
    }
}
