package net.ximatai.muyun.spring.platform.ui;

public record PlatformRecordNavigationMove(
        String sessionId,
        String currentRecordId,
        String previousRecordId,
        String nextRecordId,
        boolean first,
        boolean last
) {
}
