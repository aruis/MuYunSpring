package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.database.core.orm.Criteria;
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

    @Override
    public void beforeInsert(Teacher entity) {
        rejectDuplicate(entity, Criteria.of().eq("teacherNo", entity.getTeacherNo()),
                "teacherNo already exists in the current tenant");
    }

    @Override
    public void beforeUpdate(Teacher entity) {
        rejectDuplicate(entity, Criteria.of().eq("teacherNo", entity.getTeacherNo()),
                "teacherNo already exists in the current tenant");
    }
}
