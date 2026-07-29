package net.ximatai.muyun.spring.boot.demo.school.hobby;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;

/** 为学生多选爱好字段提供候选项的树形引用数据。 */
public class HobbyService extends AbstractAbilityService<Hobby> implements
        SoftDeleteAbility<Hobby>,
        EnableAbility<Hobby>,
        TreeAbility<Hobby>,
        CacheAbility<Hobby>,
        ReferenceAbility<Hobby> {

    public HobbyService(HobbyDao dao) {
        super("education.hobby", Hobby.class, dao);
    }
}
