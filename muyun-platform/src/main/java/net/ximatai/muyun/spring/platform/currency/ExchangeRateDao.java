package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.quarkus.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;

@MuYunRepository
public interface ExchangeRateDao extends BaseDao<ExchangeRate, String> {
}
