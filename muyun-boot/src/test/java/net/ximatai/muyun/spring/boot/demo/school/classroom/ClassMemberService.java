package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.ChildAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import org.springframework.stereotype.Service;

/**
 * 班级成员子表的标准 Service：{@link ChildAbility} 使其可被班级聚合装配，
 * {@link SortAbility} 与 {@link SoftDeleteAbility} 分别提供班内排序和成员历史保留。
 * {@link ReferencerAbility} 将班级、学生引用纳入统一的写入完整性校验与引用依赖缓存失效链路。
 */
@Service
public class ClassMemberService extends AbstractAbilityService<ClassMember> implements
        SoftDeleteAbility<ClassMember>,
        SortAbility<ClassMember>,
        ChildAbility<ClassMember>,
        ReferencerAbility<ClassMember> {

    public ClassMemberService(ClassMemberDao dao) {
        super("education.class_member", ClassMember.class, dao);
    }
}
