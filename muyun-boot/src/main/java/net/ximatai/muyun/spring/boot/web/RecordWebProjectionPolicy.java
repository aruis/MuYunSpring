package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

/** Web-only record visibility check applied before a standard projected operation executes. */
public interface RecordWebProjectionPolicy {
    void requireRecord(HttpServletRequest request, PlatformAction action, String id);
}
