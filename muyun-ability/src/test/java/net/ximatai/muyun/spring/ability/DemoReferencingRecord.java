package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceProject;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
final class DemoReferencingRecord extends StandardEntity {
    @ReferenceTo(
            moduleAlias = "demo",
            entityCode = "customer",
            autoTitle = true,
            titleOutputField = "customerTitle",
            projections = @ReferenceProject(targetField = "status", outputField = "customerStatus")
    )
    private String customerId;
    private transient String customerTitle;
    private transient String customerStatus;
    @ReferenceTo(moduleAlias = "iam", entityCode = "user", autoTitle = true, titleOutputField = "ownerTitle")
    private String ownerId;
    private transient String ownerTitle;
    @ReferenceTo(moduleAlias = "iam", entityCode = "user", cardinality = ReferenceCardinality.MANY)
    private String watcherIds;

    DemoReferencingRecord(String customerId, String ownerId) {
        this.customerId = customerId;
        this.ownerId = ownerId;
    }
}
