import type { PlatformErrorCode } from '@muyun/web-core';

const zhCNMessages: Record<string, string> = {
  AUTH_REQUIRED: '登录状态已失效，请重新登录',
  AUTH_EXPIRED: '登录会话已过期，请重新登录',
};

/**
 * Application-facing message boundary. The current platform only ships a zh-CN
 * default dictionary; future locale packs resolve the same stable error code here.
 */
export function platformMessage(code: PlatformErrorCode, fallback: string, locale = 'zh-CN') {
  if (locale === 'zh-CN') {
    return zhCNMessages[code] ?? fallback;
  }
  return fallback;
}
