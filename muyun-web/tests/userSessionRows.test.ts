import test from 'node:test';
import assert from 'node:assert/strict';
import { useUserSessionRows } from '../src/views/useUserSessionRows.ts';
import type { UserAccount, UserSessionStatusView, UserSessionView } from '../src/web-contracts/index.ts';
import type { HttpRequestOptions, ModuleContext } from '../src/web-core/index.ts';

test('user session business events coalesce visible status and expanded session refreshes', async () => {
  const requests: Array<{ path: string; body?: unknown }> = [];
  const rows = useUserSessionRows({
    context: sessionRowsContext(async (request) => {
      requests.push(request);
      if (request.path === '/iam.user/sessions/status') {
        return [{ userId: 'user-1', online: true, activeSessionCount: 1 }];
      }
      return [];
    }),
    source: 'test-user',
  });

  rows.handleUserListLoaded([{ id: 'user-1' }]);
  await flushTimers();
  rows.expandedUserKeys.value = ['user-1'];

  rows.handleUserSessionBusinessEvent(sessionChangedEvent('user-1'));
  rows.handleUserSessionBusinessEvent(sessionChangedEvent('user-1'));
  await flushTimers();

  assert.equal(requests.filter((request) => request.path === '/iam.user/sessions/status').length, 2);
  assert.equal(requests.filter((request) => request.path === '/iam.user/user-1/sessions').length, 1);
});

test('user session rows keep latest session refresh when earlier request finishes last', async () => {
  const sessionRequests: Array<{
    resolve: (records: UserSessionView[]) => void;
    promise: Promise<UserSessionView[]>;
  }> = [];
  const rows = useUserSessionRows({
    context: sessionRowsContext((request) => {
      if (request.path === '/iam.user/user-1/sessions') {
        const deferred = deferredValue<UserSessionView[]>();
        sessionRequests.push(deferred);
        return deferred.promise;
      }
      return [];
    }),
    source: 'test-user',
  });

  rows.loadUserSessions('user-1');
  await waitFor(() => sessionRequests.length === 1);
  rows.loadUserSessions('user-1');
  await waitFor(() => sessionRequests.length === 2);

  sessionRequests[1].resolve([{ id: 'session-latest' } as UserSessionView]);
  await flushPromises();
  assert.deepEqual(rows.userSessionState('user-1').records, [{ id: 'session-latest' }]);

  sessionRequests[0].resolve([{ id: 'session-stale' } as UserSessionView]);
  await flushPromises();
  assert.deepEqual(rows.userSessionState('user-1').records, [{ id: 'session-latest' }]);
});

test('user session rows keep latest online status when earlier request finishes last', async () => {
  const statusRequests: Array<{
    body: unknown;
    resolve: (records: UserSessionStatusView[]) => void;
    promise: Promise<UserSessionStatusView[]>;
  }> = [];
  const rows = useUserSessionRows({
    context: sessionRowsContext((request) => {
      if (request.path === '/iam.user/sessions/status') {
        const deferred = deferredValue<UserSessionStatusView[]>();
        statusRequests.push({ body: request.body, ...deferred });
        return deferred.promise;
      }
      return [];
    }),
    source: 'test-user',
  });

  rows.handleUserListLoaded([{ id: 'user-1' }]);
  await waitFor(() => statusRequests.length === 1);
  rows.handleUserSessionBusinessEvent(sessionChangedEvent('user-1'));
  await waitFor(() => statusRequests.length === 2);

  statusRequests[1].resolve([{ userId: 'user-1', online: true, activeSessionCount: 2 }]);
  await flushPromises();
  assert.equal(rows.userOnlineStatusTitle({ id: 'user-1' }), '在线 (2)');

  statusRequests[0].resolve([{ userId: 'user-1', online: false, activeSessionCount: 0 }]);
  await flushPromises();
  assert.equal(rows.userOnlineStatusTitle({ id: 'user-1' }), '在线 (2)');
});

function sessionRowsContext(
  request: (request: { path: string; body?: unknown }) => Promise<unknown> | unknown,
): ModuleContext<UserAccount> {
  return {
    moduleAlias: 'iam.user',
    http: {
      request: <T>(options: HttpRequestOptions) =>
        Promise.resolve(request({ path: options.path, body: options.body })) as Promise<T>,
    },
    can: (actionCode) => (actionCode === 'sessions' ? true : undefined),
    recordActions: async () => ({
      recordId: 'user-1',
      actions: [{ actionCode: 'sessions', available: true }],
    }),
  } as ModuleContext<UserAccount>;
}

function sessionChangedEvent(userId: string) {
  return {
    type: 'iam.user.session.collectionChanged',
    moduleAlias: 'iam.user',
    recordId: userId,
    reason: 'LOGGED_IN',
    sensitivity: 'DIRTY_MARKER',
  };
}

function deferredValue<T>() {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((resolver) => {
    resolve = resolver;
  });
  return { promise, resolve };
}

async function flushTimers() {
  await new Promise((resolve) => setTimeout(resolve, 5));
  await flushPromises();
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
}

async function waitFor(predicate: () => boolean) {
  for (let i = 0; i < 20; i += 1) {
    if (predicate()) {
      return;
    }
    await flushTimers();
  }
  assert.equal(predicate(), true);
}
