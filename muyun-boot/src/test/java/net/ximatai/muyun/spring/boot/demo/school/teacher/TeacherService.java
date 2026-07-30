package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import org.springframework.stereotype.Service;

/** 教师主数据 Service：为班主任和学生助理链路提供统一引用目标。 */
@Service
public class TeacherService extends AbstractAbilityService<Teacher> implements
        SoftDeleteAbility<Teacher>,
        EnableAbility<Teacher>,
        CacheAbility<Teacher>,
        ReferenceAbility<Teacher> {
    public static final String MODULE_ALIAS = "education.teacher";

    public TeacherService(TeacherDao dao) {
        super(MODULE_ALIAS, Teacher.class, dao);
    }
}
