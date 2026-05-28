package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;


final class DemoOrganizationService implements
        CrudAbility<DemoOrganization>,
        SoftDeleteAbility<DemoOrganization>,
        TreeAbility<DemoOrganization>,
        ReferenceAbility<DemoOrganization> {

    private final InMemoryBaseDao<DemoOrganization> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoOrganization, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "iam.organization";
    }
}
