package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface StudentDao extends BaseDao<Student, String> {
}
