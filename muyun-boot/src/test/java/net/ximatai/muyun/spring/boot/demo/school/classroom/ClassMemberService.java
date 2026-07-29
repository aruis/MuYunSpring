package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceLookup;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassMemberService extends AbstractAbilityService<ClassMember> implements
        SoftDeleteAbility<ClassMember>,
        SortAbility<ClassMember>,
        CascadeDeleteChildAbility<ClassMember>,
        ReferencerAbility<ClassMember> {
    private final StudentService studentService;

    public ClassMemberService(ClassMemberDao dao, StudentService studentService) {
        super("education.class_member", ClassMember.class, dao);
        this.studentService = studentService;
    }

    @Override
    public Criteria sortScope(ClassMember entity) {
        return sortScopeByFields(entity, "classroomId");
    }

    @Override
    public void validateSortScope(ClassMember left, ClassMember right) {
        validateSortScopeByFields(left, right, "class members must stay in one classroom", "classroomId");
    }

    @Override
    public List<ReferenceLookup> referenceLookups() {
        return List.of(referenceLookup(studentService));
    }
}
