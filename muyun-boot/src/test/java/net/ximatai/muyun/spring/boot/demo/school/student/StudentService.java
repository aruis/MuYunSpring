package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import org.springframework.stereotype.Service;

/**
 * 学生主数据的标准 Service：{@link ReferenceAbility} 让班级成员和教师助理字段引用学生；
 * {@link ReferencerAbility} 则让学生自身的多选爱好进入标准投影与依赖失效链路。
 * 启停、回收站和缓存均由能力组合提供，业务代码不重复实现 CRUD 流程。
 */
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

}
