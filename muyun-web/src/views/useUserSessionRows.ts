import { ref } from 'vue';
import { presentPlatformError } from '@muyun/platform-components';
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
  const userSessionChangedEventType = 'iam.user.session.changed';
  const expandedUserKeys = ref<string[]>([]);
  const userSessionStates = ref<Record<string, UserSessionState>>({});
  const visibleUserIds = ref<string[]>([]);
  const userOnlineStatuses = ref<Record<string, UserSessionStatusView>>({});
  const pendingOnlineStatusUserIds = new Set<string>();
  const pendingSessionUserIds = new Set<string>();
  const onlineStatusRequestVersions = new Map<string, number>();
  const sessionRequestVersions = new Map<string, number>();
  let onlineStatusRefreshTimer: ReturnType<typeof setTimeout> | undefined;
  let sessionRefreshTimer: ReturnType<typeof setTimeout> | undefined;
  let requestVersion = 0;

  async function loadUserSessions(userId: string | undefined) {
    if (!userId || options.context.can('sessions', userId) === false) {
      return;
    }
    const version = nextRequestVersion();
    sessionRequestVersions.set(userId, version);
    setUserSessionState(userId, { ...userSessionState(userId), loading: true, error: undefined });
    try {
      await loadUserSessionActions(userId);
      const records = await options.context.http.request<UserSessionView[]>({
        method: 'GET',
        path: `/iam.user/${encodeURIComponent(userId)}/sessions`,
      });
      if (sessionRequestVersions.get(userId) !== version) {
        return;
      }
      setUserSessionState(userId, { records, loading: false, error: undefined });
    } catch (cause) {
      if (sessionRequestVersions.get(userId) !== version) {
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
      void loadUserSessions(userId);
    }
  }

  function handleUserListLoaded(records: Array<{ id?: string }>) {
    visibleUserIds.value = records.map((record) => String(record.id ?? '')).filter(Boolean);
    void loadUserOnlineStatuses(visibleUserIds.value);
  }

  function handleUserSessionBusinessEvent(event: WebBusinessRealtimeEvent) {
    if (event.type !== userSessionChangedEventType || event.moduleAlias !== 'iam.user') {
      return;
    }
    const userId = String(event.recordId ?? '');
    if (!userId) {
      return;
    }
    if (visibleUserIds.value.includes(userId)) {
      scheduleUserOnlineStatusRefresh([userId]);
    }
    if (expandedUserKeys.value.includes(userId)) {
      scheduleUserSessionRefresh(userId);
    }
  }

  async function loadUserOnlineStatuses(userIds: string[]) {
    const ids = Array.from(new Set(userIds.filter(Boolean)));
    if (ids.length === 0) {
      return;
    }
    const version = nextRequestVersion();
    ids.forEach((id) => onlineStatusRequestVersions.set(id, version));
    try {
      const statuses = await options.context.http.request<UserSessionStatusView[]>({
        method: 'POST',
        path: '/iam.user/sessions/status',
        body: { userIds: ids },
      });
      const latestStatuses = statuses.filter(
        (status) => onlineStatusRequestVersions.get(status.userId) === version,
      );
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

  function scheduleUserOnlineStatusRefresh(userIds: string[]) {
    for (const userId of userIds) {
      if (userId) {
        pendingOnlineStatusUserIds.add(userId);
      }
    }
    if (onlineStatusRefreshTimer) {
      return;
    }
    onlineStatusRefreshTimer = setTimeout(() => {
      onlineStatusRefreshTimer = undefined;
      const ids = Array.from(pendingOnlineStatusUserIds);
      pendingOnlineStatusUserIds.clear();
      void loadUserOnlineStatuses(ids);
    }, 0);
  }

  function scheduleUserSessionRefresh(userId: string) {
    if (!userId) {
      return;
    }
    pendingSessionUserIds.add(userId);
    if (sessionRefreshTimer) {
      return;
    }
    sessionRefreshTimer = setTimeout(() => {
      sessionRefreshTimer = undefined;
      const ids = Array.from(pendingSessionUserIds);
      pendingSessionUserIds.clear();
      for (const id of ids) {
        void loadUserSessions(id);
      }
    }, 0);
  }

  function nextRequestVersion() {
    requestVersion += 1;
    return requestVersion;
  }

  function resetUserSessionRows() {
    if (onlineStatusRefreshTimer) {
      clearTimeout(onlineStatusRefreshTimer);
      onlineStatusRefreshTimer = undefined;
    }
    if (sessionRefreshTimer) {
      clearTimeout(sessionRefreshTimer);
      sessionRefreshTimer = undefined;
    }
    pendingOnlineStatusUserIds.clear();
    pendingSessionUserIds.clear();
    onlineStatusRequestVersions.clear();
    sessionRequestVersions.clear();
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
