package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoOrganizationService extends AbstractAbilityService<DemoOrganization> implements
        SoftDeleteAbility<DemoOrganization>,
        TreeAbility<DemoOrganization>,
        ReferenceAbility<DemoOrganization> {

    DemoOrganizationService() {
        super("iam.organization", new InMemoryBaseDao<>());
    }
}
