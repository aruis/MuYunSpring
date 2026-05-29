package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService extends AbstractAbilityService<Application> implements
        SoftDeleteAbility<Application>,
        EnableAbility<Application>,
        SortAbility<Application> {

    public static final String MODULE_ALIAS = "platform.application";

    public ApplicationService(BaseDao<Application, String> applicationDao) {
        super(MODULE_ALIAS, Application.class, applicationDao);
    }

    @Override
    public void beforePrepareInsert(Application application) {
        requireAlias(application.getAlias());
    }

    @Override
    public void beforeInsert(Application application) {
        requireAlias(application.getAlias());
    }

    @Override
    public void beforeUpdate(Application application) {
        requireAlias(application.getAlias());
    }

    private void requireAlias(String alias) {
        PlatformNameRules.requireApplicationAlias(alias);
    }
}
