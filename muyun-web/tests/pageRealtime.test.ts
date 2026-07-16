import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createPageBusinessEventHandler,
  createPageDataChangeHandler,
  createRealtimeRefreshQueue,
  type RealtimeRefreshRun,
} from '../src/app/pageRealtime.ts';
import type { WebBusinessRealtimeEvent } from '../src/web-contracts/index.ts';

test('page business event handler filters by type module and record', async () => {
  const handled: string[] = [];
  const handler = createPageBusinessEventHandler({
    type: 'iam.user.session.collectionChanged',
    moduleAlias: 'iam.user',
    recordId: () => 'user-1',
    handler: (event) => {
      handled.push(event.recordId);
    },
  });

  handler(businessEvent({ recordId: 'user-2' }));
  handler(businessEvent({ type: 'other.event', recordId: 'user-1' }));
  handler(businessEvent({ moduleAlias: 'iam.role', recordId: 'user-1' }));
  handler(businessEvent({ recordId: 'user-1' }));
  await flushPromises();

  assert.deepEqual(handled, ['user-1']);
});

test('page data change handler forwards only matching changes', async () => {
  const handled: string[] = [];
  const handler = createPageDataChangeHandler({
    moduleAlias: 'sales.order',
    recordId: 'order-1',
    handler: (_changeSet, changes) => {
      handled.push(...changes.map((change) => String(change.recordId)));
    },
  });

  handler({
    changeSetId: 'change-set-1',
    changes: [
      { type: 'record-updated', moduleAlias: 'sales.order', recordId: 'order-1' },
      { type: 'record-updated', moduleAlias: 'sales.order', recordId: 'order-2' },
      { type: 'record-updated', moduleAlias: 'iam.user', recordId: 'order-1' },
    ],
  });
  await flushPromises();

  assert.deepEqual(handled, ['order-1']);
});

test('realtime refresh queue coalesces keys in one flush', async () => {
  const runs: Array<RealtimeRefreshRun<string>> = [];
  const queue = createRealtimeRefreshQueue<string>({
    load: (run) => {
      runs.push(run);
    },
  });

  queue.enqueue('user-1');
  queue.enqueue(['user-1', 'user-2']);
  await flushTimers();

  assert.equal(runs.length, 1);
  assert.deepEqual(runs[0].keys, ['user-1', 'user-2']);
});

test('realtime refresh queue marks older runs stale per key', async () => {
  const runs: Array<RealtimeRefreshRun<string>> = [];
  const queue = createRealtimeRefreshQueue<string>({
    load: (run) => {
      runs.push(run);
    },
  });

  queue.enqueue('user-1');
  await waitFor(() => runs.length === 1);
  queue.enqueue('user-1');
  await waitFor(() => runs.length === 2);

  assert.equal(runs[0].isLatest('user-1'), false);
  assert.equal(runs[1].isLatest('user-1'), true);
});

test('realtime refresh queue ignores pending and active runs after dispose', async () => {
  const runs: Array<RealtimeRefreshRun<string>> = [];
  const queue = createRealtimeRefreshQueue<string>({
    load: (run) => {
      runs.push(run);
    },
  });

  queue.enqueue('user-1');
  queue.dispose();
  await flushTimers();
  assert.equal(runs.length, 0);

  const activeQueue = createRealtimeRefreshQueue<string>({
    load: (run) => {
      runs.push(run);
    },
  });
  activeQueue.enqueue('user-2');
  await waitFor(() => runs.length === 1);
  activeQueue.dispose();

  assert.equal(runs[0].active(), false);
  assert.equal(runs[0].isLatest('user-2'), false);
});

test('realtime refresh queue reset clears stale state but keeps queue reusable', async () => {
  const runs: Array<RealtimeRefreshRun<string>> = [];
  const queue = createRealtimeRefreshQueue<string>({
    load: (run) => {
      runs.push(run);
    },
  });

  queue.enqueue('user-1');
  await waitFor(() => runs.length === 1);
  queue.reset();

  assert.equal(runs[0].active(), true);
  assert.equal(runs[0].isLatest('user-1'), false);

  queue.enqueue('user-1');
  await waitFor(() => runs.length === 2);
  assert.equal(runs[1].isLatest('user-1'), true);
});

function businessEvent(overrides: Partial<WebBusinessRealtimeEvent>): WebBusinessRealtimeEvent {
  return {
    type: 'iam.user.session.collectionChanged',
    moduleAlias: 'iam.user',
    recordId: 'user-1',
    ...overrides,
  };
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
