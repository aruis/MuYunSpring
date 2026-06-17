package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface CurrencyDao extends BaseDao<Currency, String> {
}
