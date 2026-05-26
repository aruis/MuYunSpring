package net.ximatai.muyun.spring.ability;

final class DemoOrganizationService implements
        CrudAbility<DemoOrganization>,
        TreeAbility<DemoOrganization>,
        SortAbility<DemoOrganization>,
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
