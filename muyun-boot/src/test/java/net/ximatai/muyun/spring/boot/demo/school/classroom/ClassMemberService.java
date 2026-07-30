package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import org.springframework.stereotype.Service;

/** 班级成员的子表 Service：排序、软删与级联删除均由能力组合接管。 */
@Service
public class ClassMemberService extends AbstractAbilityService<ClassMember> implements
        SoftDeleteAbility<ClassMember>,
        SortAbility<ClassMember>,
        CascadeDeleteChildAbility<ClassMember>,
        ReferencerAbility<ClassMember> {

    public ClassMemberService(ClassMemberDao dao) {
        super("education.class_member", ClassMember.class, dao);
    }
}
