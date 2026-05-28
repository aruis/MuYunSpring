package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;

final class DemoUserService extends AbstractAbilityService<DemoUser> implements ReferenceAbility<DemoUser> {
    DemoUserService() {
        super("iam.user", DemoUser.class, new InMemoryBaseDao<>());
    }
}
