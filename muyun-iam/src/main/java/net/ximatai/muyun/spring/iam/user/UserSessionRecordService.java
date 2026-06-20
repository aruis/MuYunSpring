package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserSessionRecordService extends AbstractAbilityService<UserSession> {
    public static final String MODULE_ALIAS = "iam.user_session";
    private static final PageRequest FIRST = new PageRequest(0, 1);
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    public UserSessionRecordService(UserSessionDao userSessionDao) {
        super(MODULE_ALIAS, UserSession.class, userSessionDao);
    }

    public String issue(UserSession session) {
        return insert(session);
    }

    public UserSession findByTokenHash(String tokenHash) {
        return list(Criteria.of().eq("tokenHash", tokenHash), FIRST)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public UserSession findById(String id) {
        return select(id);
    }

    public List<UserSession> listByUserId(String userId) {
        return list(Criteria.of().eq("userId", userId), ALL);
    }

    public int updateSession(UserSession session, Integer expectedVersion, Instant now) {
        EntityLifecycle.prepareUpdate(session, now);
        return getDao().updateByIdAndVersion(session, expectedVersion);
    }
}
