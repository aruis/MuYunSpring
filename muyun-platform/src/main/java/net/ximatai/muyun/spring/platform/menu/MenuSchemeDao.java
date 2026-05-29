package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface MenuSchemeDao extends BaseDao<MenuScheme, String> {
}
