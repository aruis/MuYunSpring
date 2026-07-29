package net.ximatai.muyun.spring.boot.demo.school.hobby;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface HobbyDao extends BaseDao<Hobby, String> {
}
