package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import org.springframework.stereotype.Service;

/** 学生主数据 Service：既作为引用目标，也作为爱好引用与班级成员反查的读取来源。 */
@Service
public class StudentService extends AbstractAbilityService<Student> implements
        RecycleBinAbility<Student>,
        EnableAbility<Student>,
        CacheAbility<Student>,
        ReferenceAbility<Student>,
        ReferencerAbility<Student> {
    public static final String MODULE_ALIAS = "education.student";

    public StudentService(StudentDao dao) {
        super(MODULE_ALIAS, Student.class, dao);
    }

    @Override
    public String getDeletionEntityAlias() {
        return "student";
    }

}
