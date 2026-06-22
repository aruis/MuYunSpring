import test from 'node:test';
import assert from 'node:assert/strict';
import {
  AppError,
  createAuthClient,
  createHttpClient,
  normalizeError,
  platformErrorCodes,
  resolveGlobalErrorPresentation,
} from '../src/web-core/index.ts';

test('auth logout posts bearer token to backend logout endpoint', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    requests.push(new Request(input, init));
    return new Response(null, { status: 200 });
  };

  try {
    const authClient = createAuthClient(createHttpClient({ baseUrl: 'http://api.local' }));

    await authClient.logout('token-1');

    assert.equal(requests.length, 1);
    assert.equal(requests[0].url, 'http://api.local/iam.auth/logout');
    assert.equal(requests[0].method, 'POST');
    assert.equal(requests[0].headers.get('Authorization'), 'Bearer token-1');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('http client sends platform trace header', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    requests.push(new Request(input, init));
    return Response.json({ ok: true });
  };

  try {
    const http = createHttpClient({ baseUrl: 'http://api.local', traceId: 'trace-client' });

    await http.request({ path: '/platform.ping' });

    assert.equal(requests[0].headers.get('X-MuYun-Trace-Id'), 'trace-client');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('http client maps unified backend error envelope to AppError facts', async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    Response.json(
      {
        traceId: 'trace-body',
        code: 'DYNAMIC_FIELD_REQUIRED',
        status: 422,
        message: '客户名称不能为空',
        scope: { moduleAlias: 'crm.customer' },
        targets: [{ kind: 'field', fieldName: 'customerName' }],
        details: { rule: 'required' },
      },
      { status: 422 },
    );

  try {
    const http = createHttpClient({ baseUrl: 'http://api.local' });

    await assert.rejects(
      () => http.request({ path: '/dynamic/crm.customer/records' }),
      (error) => {
        assert.equal(error instanceof AppError, true);
        const appError = error as AppError;
        assert.equal(appError.message, '客户名称不能为空');
        assert.equal(appError.code, 'DYNAMIC_FIELD_REQUIRED');
        assert.equal(appError.status, 422);
        assert.equal(appError.traceId, 'trace-body');
        assert.deepEqual(appError.scope, { moduleAlias: 'crm.customer' });
        assert.deepEqual(appError.targets, [{ kind: 'field', fieldName: 'customerName' }]);
        assert.deepEqual(appError.details, { rule: 'required' });
        return true;
      },
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('http client falls back to response trace header for AppError traceId', async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    Response.json(
      {
        code: platformErrorCodes.configMissing,
        status: 409,
        message: '菜单方案未配置',
      },
      { status: 409, headers: { 'X-MuYun-Trace-Id': 'trace-header' } },
    );

  try {
    const http = createHttpClient({ baseUrl: 'http://api.local' });

    await assert.rejects(
      () => http.request({ path: '/platform.menu/mine' }),
      (error) => {
        assert.equal(error instanceof AppError, true);
        assert.equal((error as AppError).traceId, 'trace-header');
        return true;
      },
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('http client wraps invalid json error response as AppError', async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response('{', {
      status: 500,
      headers: { 'Content-Type': 'application/json', 'X-MuYun-Trace-Id': 'trace-invalid-json' },
    });

  try {
    const http = createHttpClient({ baseUrl: 'http://api.local' });

    await assert.rejects(
      () => http.request({ path: '/platform.broken' }),
      (error) => {
        assert.equal(error instanceof AppError, true);
        const appError = error as AppError;
        assert.equal(appError.code, platformErrorCodes.httpError);
        assert.equal(appError.status, 500);
        assert.equal(appError.traceId, 'trace-invalid-json');
        assert.match(String(appError.details?.cause), /JSON/);
        return true;
      },
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('normalizeError keeps AppError and wraps unknown errors', () => {
  const appError = new AppError('conflict', { code: platformErrorCodes.conflictVersion, status: 409 });

  assert.equal(normalizeError(appError), appError);
  assert.deepEqual(normalizeError(new Error('boom')), new AppError('boom', { code: platformErrorCodes.appError }));
  assert.deepEqual(normalizeError('boom').details, { cause: 'boom' });
});

test('resolveGlobalErrorPresentation maps common failures to fixed global slots', () => {
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('login required', { status: 401 }), {
      phase: 'action',
      surface: 'shell',
    }).slot,
    'redirect-login',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('bad credentials', { code: platformErrorCodes.loginBadCredentials, status: 401 }), {
      phase: 'action',
      surface: 'form',
    }).slot,
    'global-toast',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('forbidden', { status: 403 }), {
      phase: 'page-load',
      surface: 'shell',
    }).slot,
    'page-error',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('conflict', { status: 409 }), {
      phase: 'action',
      surface: 'form',
    }).slot,
    'global-modal',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('failed', { status: 500 }), {
      phase: 'page-load',
      surface: 'shell',
    }).slot,
    'page-error',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('failed', { status: 500 }), {
      phase: 'background',
      surface: 'unknown',
    }).slot,
    'silent',
  );
  assert.equal(resolveGlobalErrorPresentation(new AppError('bad request', { status: 400 })).slot, 'global-toast');
});
