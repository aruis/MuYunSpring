package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import org.springframework.stereotype.Service;

@Service
public class TestPersonService extends AbstractAbilityService<TestPerson> implements
        GlobalScopedAbility<TestPerson> {

    public static final String MODULE_ALIAS = "test.person";

    public TestPersonService(TestPersonDao dao) {
        super(MODULE_ALIAS, TestPerson.class, dao);
    }
}
