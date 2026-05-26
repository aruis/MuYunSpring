package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService implements
        CrudAbility<Organization>,
        TreeAbility<Organization>,
        SortAbility<Organization>,
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

    @Override
    public void beforeInsert(Organization entity) {
        if (entity.getEnabled() == null) {
            entity.setEnabled(Boolean.TRUE);
        }
        if (entity.getParentId() == null || entity.getParentId().isBlank()) {
            entity.setParentId(TreeAbility.ROOT_ID);
        }
    }
}
