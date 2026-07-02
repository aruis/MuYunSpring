package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface MenuDao extends BaseDao<Menu, String> {
}
