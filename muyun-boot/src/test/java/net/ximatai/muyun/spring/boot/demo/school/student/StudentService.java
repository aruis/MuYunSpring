package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceLookup;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.boot.demo.school.hobby.HobbyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService extends AbstractAbilityService<Student> implements
        RecycleBinAbility<Student>,
        EnableAbility<Student>,
        CacheAbility<Student>,
        ReferenceAbility<Student>,
        ReferencerAbility<Student> {
    private final HobbyService hobbyService;

    public StudentService(StudentDao dao, HobbyService hobbyService) {
        super("education.student", Student.class, dao);
        this.hobbyService = hobbyService;
    }

    @Override
    public String getDeletionEntityAlias() {
        return "student";
    }

    @Override
    public List<ReferenceLookup> referenceLookups() {
        return List.of(referenceLookup(hobbyService));
    }
}
