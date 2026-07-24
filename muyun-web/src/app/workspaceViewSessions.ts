import type { WorkspaceViewDefinition, WorkspaceViewInput } from './workspaceViewContract';

/**
 * Process-local session slots bridge a view while its host changes. They hold
 * only interactive state; the URL remains the source of restorable identity.
 */
const sessions = new Map<string, unknown>();
const handoffRecipients = new Map<string, Set<WorkspaceViewHandoffRecipient<unknown>>>();

/** A mounted workspace view may accept a source drawer's in-memory editing state. */
export type WorkspaceViewHandoffRecipient<TSession> = (session: TSession) => boolean | Promise<boolean>;

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

/**
 * Delivers a workspace hand-off to an already mounted target when possible;
 * otherwise stores it for the target that is about to mount. A recipient can
 * reject the delivery when it owns conflicting, unsaved state.
 */
export async function handOffWorkspaceViewSession<TInput extends WorkspaceViewInput, TSession>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
  session: TSession,
) {
  const key = workspaceViewInstanceKey(view, input);
  const recipients = handoffRecipients.get(key);
  const recipient = recipients && [...recipients].at(-1);
  if (recipient) {
    return (await recipient(cloneWorkspaceViewSession(session))) ? 'accepted' : 'rejected';
  }
  sessions.set(key, cloneWorkspaceViewSession(session));
  return 'accepted';
}

/** Registers the mounted host that can receive a hand-off for one stable workspace identity. */
export function registerWorkspaceViewHandoffRecipient<TInput extends WorkspaceViewInput, TSession>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
  recipient: WorkspaceViewHandoffRecipient<TSession>,
) {
  const key = workspaceViewInstanceKey(view, input);
  const recipients = handoffRecipients.get(key) ?? new Set<WorkspaceViewHandoffRecipient<unknown>>();
  recipients.add(recipient as WorkspaceViewHandoffRecipient<unknown>);
  handoffRecipients.set(key, recipients);
  return () => {
    recipients.delete(recipient as WorkspaceViewHandoffRecipient<unknown>);
    if (recipients.size === 0) handoffRecipients.delete(key);
  };
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

/**
 * Workspace drafts are platform JSON contracts. JSON round-tripping both
 * detaches the source host and safely unwraps Vue's reactive proxies, which
 * native structuredClone intentionally rejects.
 */
function cloneWorkspaceViewSession<TSession>(session: TSession): TSession {
  return JSON.parse(JSON.stringify(session)) as TSession;
}
