package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.spring.boot.sql.annotation.MuYunRepository;
import net.ximatai.muyun.spring.ability.BaseDao;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@MuYunRepository
public interface OrganizationDao extends BaseDao<Organization, String> {
    @SqlUpdate("update iam_organization set name = :name where id = :id")
    int rename(@Bind("id") String id, @Bind("name") String name);
}
