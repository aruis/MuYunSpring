import { ref } from 'vue';
import { presentPlatformError } from '@muyun/platform-components';
export { userSessionPresenceTitle } from '@muyun/platform-components';
import { createRealtimeRefreshQueue } from '../platform-admin-runtime/pageRealtime';
import type {
  UserAccount,
  UserSessionStatusView,
  UserSessionView,
  WebBusinessRealtimeEvent,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';

export interface UserSessionState {
  records: UserSessionView[];
  loading: boolean;
  error?: string;
}

export interface UserSessionRowsOptions {
  context: ModuleContext<UserAccount>;
  source: string;
}

export function useUserSessionRows(options: UserSessionRowsOptions) {
  const userSessionCollectionChangedEventType = 'iam.user.session.collectionChanged';
  const expandedUserKeys = ref<string[]>([]);
  const userSessionStates = ref<Record<string, UserSessionState>>({});
  const visibleUserIds = ref<string[]>([]);
  const userOnlineStatuses = ref<Record<string, UserSessionStatusView>>({});
  const onlineStatusRefreshQueue = createRealtimeRefreshQueue<string>({
    load: async (run) => {
      await loadUserOnlineStatusesNow(run.keys, run.isLatest);
    },
  });
  const sessionRefreshQueue = createRealtimeRefreshQueue<string>({
    load: async (run) => {
      await Promise.all(run.keys.map((userId) => loadUserSessionsNow(userId, run.isLatest)));
    },
  });

  function loadUserSessions(userId: string | undefined) {
    sessionRefreshQueue.enqueue(userId);
  }

  async function loadUserSessionsNow(
    userId: string | undefined,
    isLatest: (userId: string) => boolean = () => true,
  ) {
    if (!userId || options.context.can('sessions', userId) === false) {
      return;
    }
    setUserSessionState(userId, { ...userSessionState(userId), loading: true, error: undefined });
    try {
      await loadUserSessionActions(userId);
      const records = await options.context.http.request<UserSessionView[]>({
        method: 'GET',
        path: `/iam.user/${encodeURIComponent(userId)}/sessions`,
      });
      if (!isLatest(userId)) {
        return;
      }
      setUserSessionState(userId, { records, loading: false, error: undefined });
    } catch (cause) {
      if (!isLatest(userId)) {
        return;
      }
      const error = presentPlatformError(cause, { source: `${options.source}-sessions`, phase: 'load' });
      setUserSessionState(userId, { ...userSessionState(userId), loading: false, error: error.message });
    }
  }

  async function loadUserSessionActions(userId: string) {
    try {
      await options.context.recordActions(userId);
    } catch (cause) {
      presentPlatformError(cause, { source: `${options.source}-sessions`, phase: 'load' });
    }
  }

  function handleUserRowExpand(record: { id?: string }, expanded: boolean) {
    const userId = String(record.id ?? '');
    if (!userId) {
      return;
    }
    expandedUserKeys.value = expanded
      ? Array.from(new Set([...expandedUserKeys.value, userId]))
      : expandedUserKeys.value.filter((key) => key !== userId);
    if (expanded && userSessionState(userId).records.length === 0) {
      sessionRefreshQueue.enqueue(userId);
    }
  }

  function handleUserListLoaded(records: Array<{ id?: string }>) {
    visibleUserIds.value = records.map((record) => String(record.id ?? '')).filter(Boolean);
    onlineStatusRefreshQueue.enqueue(visibleUserIds.value);
  }

  function handleUserSessionBusinessEvent(event: WebBusinessRealtimeEvent) {
    if (event.type !== userSessionCollectionChangedEventType || event.moduleAlias !== 'iam.user') {
      return;
    }
    const userId = String(event.recordId ?? '');
    if (!userId) {
      return;
    }
    if (visibleUserIds.value.includes(userId)) {
      onlineStatusRefreshQueue.enqueue(userId);
    }
    if (expandedUserKeys.value.includes(userId)) {
      sessionRefreshQueue.enqueue(userId);
    }
  }

  async function loadUserOnlineStatusesNow(
    userIds: string[],
    isLatest: (userId: string) => boolean = () => true,
  ) {
    const ids = Array.from(new Set(userIds.filter(Boolean)));
    if (ids.length === 0) {
      return;
    }
    try {
      const statuses = await options.context.http.request<UserSessionStatusView[]>({
        method: 'POST',
        path: '/iam.user/sessions/status',
        body: { userIds: ids },
      });
      const latestStatuses = statuses.filter((status) => isLatest(status.userId));
      if (latestStatuses.length === 0) {
        return;
      }
      userOnlineStatuses.value = {
        ...userOnlineStatuses.value,
        ...Object.fromEntries(latestStatuses.map((status) => [status.userId, status])),
      };
    } catch (cause) {
      presentPlatformError(cause, { source: `${options.source}-online-status`, phase: 'load' });
    }
  }

  function userOnlineStatusTitle(record: { id?: string }) {
    const userId = String(record.id ?? '');
    const status = userOnlineStatuses.value[userId];
    if (!status) {
      return '-';
    }
    if (status.present) {
      const presentCount = status.presentSessionCount ?? 0;
      const idleCount = status.idleSessionCount ?? 0;
      if (presentCount > 0 && idleCount >= presentCount) {
        return `闲置 (${presentCount}/${status.activeSessionCount})`;
      }
      return `使用中 (${Math.max(0, presentCount - idleCount)}/${status.activeSessionCount})`;
    }
    return status.online ? `在线 (${status.activeSessionCount})` : '离线';
  }

  function userSessionState(userId: string | undefined): UserSessionState {
    if (!userId) {
      return { records: [], loading: false };
    }
    return userSessionStates.value[userId] ?? { records: [], loading: false };
  }

  function setUserSessionState(userId: string, state: UserSessionState) {
    userSessionStates.value = {
      ...userSessionStates.value,
      [userId]: state,
    };
  }

  function resetUserSessionRows() {
    onlineStatusRefreshQueue.reset();
    sessionRefreshQueue.reset();
    expandedUserKeys.value = [];
    userSessionStates.value = {};
    visibleUserIds.value = [];
    userOnlineStatuses.value = {};
  }

  return {
    expandedUserKeys,
    handleUserListLoaded,
    handleUserRowExpand,
    handleUserSessionBusinessEvent,
    loadUserSessions,
    resetUserSessionRows,
    userOnlineStatusTitle,
    userSessionState,
  };
}
