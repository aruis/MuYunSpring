package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.PageRequest;

public final class PageRequests {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private PageRequests() {
    }

    public static PageRequest all() {
        return ALL;
    }
}
