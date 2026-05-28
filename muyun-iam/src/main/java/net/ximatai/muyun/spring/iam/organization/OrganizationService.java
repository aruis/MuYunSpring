package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService extends AbstractAbilityService<Organization> implements
        SoftDeleteAbility<Organization>,
        EnableAbility<Organization>,
        TreeAbility<Organization>,
        ReferenceAbility<Organization> {

    public static final String MODULE_ALIAS = "iam.organization";

    public OrganizationService(OrganizationDao organizationDao) {
        super(MODULE_ALIAS, organizationDao);
    }
}
