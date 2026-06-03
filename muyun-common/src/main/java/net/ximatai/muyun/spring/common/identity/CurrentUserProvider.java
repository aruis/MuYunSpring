package net.ximatai.muyun.spring.common.identity;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<CurrentUser> currentUser();
}
