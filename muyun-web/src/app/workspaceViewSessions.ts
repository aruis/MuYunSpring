import type { WorkspaceViewDefinition, WorkspaceViewInput } from './workspaceViewContract';

/**
 * Process-local session slots bridge a view while its host changes. They hold
 * only interactive state; the URL remains the source of restorable identity.
 */
const sessions = new Map<string, unknown>();

export function workspaceViewInstanceKey<TInput extends WorkspaceViewInput>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
) {
  return `${view.type}:${stableJson(input)}`;
}

export function getOrCreateWorkspaceViewSession<TInput extends WorkspaceViewInput, TSession>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
  create: () => TSession,
) {
  const key = workspaceViewInstanceKey(view, input);
  const existing = sessions.get(key) as TSession | undefined;
  if (existing) return existing;
  const session = create();
  sessions.set(key, session);
  return session;
}

export function discardWorkspaceViewSession<TInput extends WorkspaceViewInput>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
) {
  sessions.delete(workspaceViewInstanceKey(view, input));
}

/** Replaces a pending hand-off session before the target host is mounted. */
export function replaceWorkspaceViewSession<TInput extends WorkspaceViewInput, TSession>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
  session: TSession,
) {
  sessions.set(workspaceViewInstanceKey(view, input), session);
}

/** Consumes a one-time hand-off session; later deep links reload from the URL. */
export function takeWorkspaceViewSession<TInput extends WorkspaceViewInput, TSession>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
) {
  const key = workspaceViewInstanceKey(view, input);
  const session = sessions.get(key) as TSession | undefined;
  sessions.delete(key);
  return session;
}

function stableJson(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`;
  if (value && typeof value === 'object') {
    return `{${Object.entries(value as Record<string, unknown>)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, entry]) => `${JSON.stringify(key)}:${stableJson(entry)}`)
      .join(',')}}`;
  }
  return JSON.stringify(value);
}
