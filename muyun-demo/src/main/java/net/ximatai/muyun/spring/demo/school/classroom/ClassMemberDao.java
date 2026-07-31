package net.ximatai.muyun.spring.demo.school.classroom;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface ClassMemberDao extends BaseDao<ClassMember, String> {
}
