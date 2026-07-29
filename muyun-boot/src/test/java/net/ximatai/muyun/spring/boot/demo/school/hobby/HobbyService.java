package net.ximatai.muyun.spring.boot.demo.school.hobby;

import net.ximatai.muyun.database.core.orm.Criteria;
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

    @Override
    public void beforeInsert(Hobby entity) {
        rejectDuplicate(entity, Criteria.of().eq("code", entity.getCode()),
                "hobby code already exists in the current tenant");
    }

    @Override
    public void beforeUpdate(Hobby entity) {
        rejectDuplicate(entity, Criteria.of().eq("code", entity.getCode()),
                "hobby code already exists in the current tenant");
    }
}
