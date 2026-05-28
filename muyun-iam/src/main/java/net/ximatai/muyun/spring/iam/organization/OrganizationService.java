package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService implements
        CrudAbility<Organization>,
        SoftDeleteAbility<Organization>,
        EnableAbility<Organization>,
        TreeAbility<Organization>,
        ReferenceAbility<Organization> {

    public static final String MODULE_ALIAS = "iam.organization";

    private final OrganizationDao organizationDao;

    public OrganizationService(OrganizationDao organizationDao) {
        this.organizationDao = organizationDao;
    }

    @Override
    public BaseDao<Organization, String> getDao() {
        return organizationDao;
    }

    @Override
    public String getModuleAlias() {
        return MODULE_ALIAS;
    }
}
