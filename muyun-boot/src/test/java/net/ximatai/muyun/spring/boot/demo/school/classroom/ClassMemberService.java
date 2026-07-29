package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.child.CascadeDeleteChildAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceLookup;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.boot.demo.school.student.Student;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentService;

import java.util.List;

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
    public void beforeInsert(ClassMember entity) {
        syncMemberTitle(entity);
    }

    @Override
    public void beforeUpdate(ClassMember entity) {
        syncMemberTitle(entity);
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

    private void syncMemberTitle(ClassMember entity) {
        Student student = studentService.select(entity.getStudentId());
        if (student == null) {
            throw new PlatformException("student does not exist: " + entity.getStudentId());
        }
        entity.setTitle(student.getTitle());
    }
}
