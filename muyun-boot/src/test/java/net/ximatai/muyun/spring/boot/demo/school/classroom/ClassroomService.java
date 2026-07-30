package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceLookup;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomService extends AbstractAbilityService<Classroom> implements
        RecycleBinAbility<Classroom>,
        SortAbility<Classroom>,
        ChildrenAbility<Classroom>,
        ReferencerAbility<Classroom>,
        ReferenceAbility<Classroom>,
        CacheAbility<Classroom> {
    private final TeacherService teacherService;
    private final ClassMemberService memberService;

    public ClassroomService(ClassroomDao dao,
                            TeacherService teacherService,
                            ClassMemberService memberService) {
        super("education.classroom", Classroom.class, dao);
        this.teacherService = teacherService;
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

    @Override
    public List<ReferenceLookup> referenceLookups() {
        return List.of(referenceLookup(teacherService));
    }
}
