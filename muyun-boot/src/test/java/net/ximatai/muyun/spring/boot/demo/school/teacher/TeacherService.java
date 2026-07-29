package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;

public class TeacherService extends AbstractAbilityService<Teacher> implements
        SoftDeleteAbility<Teacher>,
        EnableAbility<Teacher>,
        CacheAbility<Teacher>,
        ReferenceAbility<Teacher> {

    public TeacherService(TeacherDao dao) {
        super("education.teacher", Teacher.class, dao);
    }
}
