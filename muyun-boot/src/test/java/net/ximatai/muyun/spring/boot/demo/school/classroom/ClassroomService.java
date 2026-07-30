package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import org.springframework.stereotype.Service;

import java.util.List;

/** 班级聚合根 Service：声明成员关系，并作为可引用、可排序、可回收的班级主数据入口。 */
@Service
public class ClassroomService extends AbstractAbilityService<Classroom> implements
        RecycleBinAbility<Classroom>,
        SortAbility<Classroom>,
        ChildrenAbility<Classroom>,
        ReferencerAbility<Classroom>,
        ReferenceAbility<Classroom>,
        CacheAbility<Classroom> {
    public static final String MODULE_ALIAS = "education.classroom";
    private final ClassMemberService memberService;

    public ClassroomService(ClassroomDao dao,
                            ClassMemberService memberService) {
        super(MODULE_ALIAS, Classroom.class, dao);
        this.memberService = memberService;
    }

    @Override
    public String getDeletionEntityAlias() {
        return "classroom";
    }

    @Override
    public List<ChildRelation<? extends EntityContract, Classroom>> childRelations() {
        return List.of(childRelation("members", memberService));
    }

}
