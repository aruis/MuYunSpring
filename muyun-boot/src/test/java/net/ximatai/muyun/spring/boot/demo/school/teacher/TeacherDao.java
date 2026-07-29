package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface TeacherDao extends BaseDao<Teacher, String> {
}
