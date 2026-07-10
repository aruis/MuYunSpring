import test from 'node:test';
import assert from 'node:assert/strict';
import { computed, nextTick } from 'vue';
import {
  AppError,
  configureModuleContext,
  createAuthClient,
  createHttpClient,
  createModuleContext,
  createModuleTreeContext,
  createStaticModuleTreeClient,
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

test('auth change own password posts bearer token to backend endpoint', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    requests.push(new Request(input, init));
    return new Response(null, { status: 200 });
  };

  try {
    const authClient = createAuthClient(createHttpClient({ baseUrl: 'http://api.local' }));

    await authClient.changeOwnPassword(
      { currentPassword: 'old-secret', newPassword: 'new-secret' },
      'token-1',
    );

    assert.equal(requests.length, 1);
    assert.equal(requests[0].url, 'http://api.local/iam.auth/changeOwnPassword');
    assert.equal(requests[0].method, 'POST');
    assert.equal(requests[0].headers.get('Authorization'), 'Bearer token-1');
    assert.deepEqual(await requests[0].json(), {
      currentPassword: 'old-secret',
      newPassword: 'new-secret',
    });
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

test('static module tree client maps standard CRUD and tree endpoints by module alias', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    requests.push(request);
    const url = new URL(request.url);
    if (url.pathname.endsWith('/tree') && url.searchParams.get('flat') === 'true') {
      return Response.json({ records: [] });
    }
    if (url.pathname.endsWith('/query/schema')) {
      return Response.json({
        scopeName: 'iam.organization',
        quickSearch: { enabled: false, fields: [], fieldSchemas: [] },
        fields: [],
        externalCriteria: [],
        defaultSorts: [],
      });
    }
    if (request.url.endsWith('/insert')) {
      return Response.json({
        record: { id: 'org-1', title: '总部' },
        message: '已创建',
        resultType: 'created',
        effects: [{ type: 'refresh-list', payload: { moduleAlias: 'iam.organization' } }],
      });
    }
    return Response.json({ count: 1 });
  };

  try {
    const client = createStaticModuleTreeClient(createHttpClient({ baseUrl: 'http://api.local' }), {
      moduleAlias: 'iam.organization',
    });

    await client.treeFlat();
    await client.querySchema();
    await client.querySchema({ uiConfigId: 'org-list-v1' });
    const insertResult = await client.insert({ title: '总部' });
    await client.sort('org-1', { parentId: 'root' });

    assert.equal(requests[0].url, 'http://api.local/iam.organization/tree?flat=true');
    assert.equal(requests[0].method, 'GET');
    assert.equal(requests[1].url, 'http://api.local/iam.organization/query/schema');
    assert.equal(requests[1].method, 'GET');
    assert.equal(requests[2].url, 'http://api.local/iam.organization/query/schema?uiConfigId=org-list-v1');
    assert.equal(requests[2].method, 'GET');
    assert.equal(requests[3].url, 'http://api.local/iam.organization/insert');
    assert.equal(requests[3].method, 'POST');
    assert.deepEqual(await requests[3].json(), { title: '总部' });
    assert.deepEqual(insertResult, {
      record: { id: 'org-1', title: '总部' },
      message: '已创建',
      resultType: 'created',
      effects: [{ type: 'refresh-list', payload: { moduleAlias: 'iam.organization' } }],
    });
    assert.equal(requests[4].url, 'http://api.local/iam.organization/sort/org-1');
    assert.deepEqual(await requests[4].json(), { parentId: 'root' });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module context creates standard CRUD capabilities from configured http factory', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    requests.push(request);
    if (request.url.endsWith('/context')) {
      return Response.json({
        ...runtimeContext(),
        moduleAlias: 'iam.user',
        actions: [
          ...runtimeContext().actions,
          {
            actionCode: 'resetPassword',
            permissionActionCode: 'resetPassword',
            title: 'Reset Password',
            authorized: true,
          },
        ],
      });
    }
    return Response.json({ records: [] });
  };

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleContext({ moduleAlias: 'iam.organization' });

    await context.runtime.ready;
    await context.abilities.crud().query({ keyword: '总部' });

    assert.equal(context.moduleAlias, 'iam.organization');
    assert.equal(requests[0].url, 'http://api.local/platform.module/iam.organization/context');
    assert.equal(requests[1].url, 'http://api.local/iam.organization/query');
    assert.equal(requests[1].method, 'POST');
    assert.deepEqual(await requests[1].json(), { keyword: '总部' });
    assert.equal(context.runtime.can('update'), true);
    assert.equal(context.runtime.action('update')?.available, true);
    assert.equal(context.runtime.action('update')?.title, 'Update');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module runtime authorization updates Vue computed state after context loads', async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => Response.json(runtimeContext());

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleContext({ moduleAlias: 'iam.organization' });
    const canCreate = computed(() => context.can('create') === true);

    assert.equal(canCreate.value, false);

    await context.runtime.ready;
    await nextTick();

    assert.equal(canCreate.value, true);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module context resolves record action availability by record id', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    requests.push(request);
    if (request.url.endsWith('/context')) {
      return Response.json({
        ...runtimeContext(),
        actions: [
          ...runtimeContext().actions,
          {
            actionCode: 'resetPassword',
            permissionActionCode: 'resetPassword',
            title: 'Reset Password',
            authorized: true,
          },
        ],
      });
    }
    return Response.json({
      recordId: 'platform.user.super_admin',
      actions: [
        { actionCode: 'update', available: true },
        {
          actionCode: 'resetPassword',
          available: false,
          reason: "cannot administrate current user's password",
        },
      ],
    });
  };

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleContext({ moduleAlias: 'iam.user' });

    await context.runtime.ready;

    assert.equal(context.can('update'), true);
    assert.equal(context.action('update')?.available, true);
    assert.equal(context.can('resetPassword', 'platform.user.super_admin'), undefined);
    assert.equal(context.action('resetPassword', 'platform.user.super_admin'), undefined);

    const availability = await context.recordActions('platform.user.super_admin');

    assert.equal(requests[1].url, 'http://api.local/iam.user/actions/platform.user.super_admin');
    assert.equal(availability.recordId, 'platform.user.super_admin');
    assert.equal(context.can('update', 'platform.user.super_admin'), true);
    assert.equal(context.action('update', 'platform.user.super_admin')?.available, true);
    assert.equal(context.can('resetPassword', 'platform.user.super_admin'), false);
    assert.deepEqual(
      {
        available: context.action('resetPassword', 'platform.user.super_admin')?.available,
        reason: context.action('resetPassword', 'platform.user.super_admin')?.reason,
      },
      {
        available: false,
        reason: "cannot administrate current user's password",
      },
    );
    assert.equal(
      context.recordActionsSnapshot('platform.user.super_admin')?.actions[1]?.reason,
      "cannot administrate current user's password",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module context ignores record action decisions without runtime definition', async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    if (request.url.endsWith('/context')) {
      return Response.json(runtimeContext());
    }
    return Response.json({
      recordId: 'org-1',
      actions: [{ actionCode: 'ghost', available: true }],
    });
  };

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleContext({ moduleAlias: 'iam.organization' });

    await context.runtime.ready;
    await context.recordActions('org-1');

    assert.equal(context.can('ghost', 'org-1'), undefined);
    assert.equal(context.action('ghost', 'org-1'), undefined);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module context abilities compose tree and enable capabilities', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    requests.push(request);
    if (request.url.endsWith('/context')) {
      return Response.json(runtimeContext());
    }
    return Response.json({ records: [] });
  };

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleContext<{ id?: string }>({ moduleAlias: 'iam.organization' });
    assert.equal(context.abilities.tryTree(), undefined);
    assert.throws(() => context.abilities.tree(), /Module runtime context is not ready/);

    await context.runtime.ready;
    const tree = context.abilities.tree();
    const enable = context.abilities.enable();

    await context.abilities.crud().query({ keyword: '总部' });
    await tree.tree();
    await enable.disable('org-1');

    assert.equal(context.moduleAlias, 'iam.organization');
    assert.equal(requests[0].url, 'http://api.local/platform.module/iam.organization/context');
    assert.equal(requests[1].url, 'http://api.local/iam.organization/query');
    assert.equal(requests[2].url, 'http://api.local/iam.organization/tree');
    assert.equal(requests[3].url, 'http://api.local/iam.organization/disable/org-1');
    assert.equal(context.abilities.hasTree(), true);
    assert.equal(context.abilities.has('tree'), true);
    assert.equal(context.abilities.tryTree(), tree);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module tree context remains compatible with explicit tree opt-in', async () => {
  const requests: Request[] = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    const request = new Request(input, init);
    requests.push(request);
    if (request.url.endsWith('/context')) {
      return Response.json(runtimeContext());
    }
    return Response.json({ records: [] });
  };

  try {
    configureModuleContext({
      httpFactory: () => createHttpClient({ baseUrl: 'http://api.local' }),
    });

    const context = createModuleTreeContext({ moduleAlias: 'iam.organization' });

    await context.tree.tree();
    await context.runtime.ready;

    assert.equal(requests[0].url, 'http://api.local/platform.module/iam.organization/context');
    assert.equal(requests[1].url, 'http://api.local/iam.organization/tree');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('module runtime context records background load errors and retries explicit load', async () => {
  const originalFetch = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => {
    calls += 1;
    if (calls === 1) {
      return Response.json(
        {
          code: platformErrorCodes.accessDenied,
          status: 403,
          message: '权限不足',
        },
        { status: 403 },
      );
    }
    return Response.json(runtimeContext());
  };

  try {
    const context = createModuleContext({ moduleAlias: 'iam.organization', http: createHttpClient() });

    await assert.rejects(() => context.runtime.ready);

    assert.equal(context.runtime.error()?.code, platformErrorCodes.accessDenied);

    const loaded = await context.runtime.load();

    assert.equal(loaded.moduleAlias, 'iam.organization');
    assert.equal(context.runtime.error(), undefined);
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

function runtimeContext() {
  return {
    moduleAlias: 'iam.organization',
    title: '组织管理',
    moduleKind: 'STATIC',
    entryType: 'route',
    entryRoute: '/iam/organizations',
    mainEntityAlias: 'organization',
    capabilities: ['CRUD', 'SOFT_DELETE', 'LIFECYCLE', 'CACHE', 'TREE', 'SORT', 'ENABLE'],
    abilities: ['crud', 'softDelete', 'lifecycle', 'cache', 'tree', 'sort', 'enable'],
    actions: [
      { actionCode: 'query', permissionActionCode: 'view', title: 'Query', authorized: true },
      { actionCode: 'create', permissionActionCode: 'create', title: 'Create', authorized: true },
      { actionCode: 'update', permissionActionCode: 'update', title: 'Update', authorized: true },
      { actionCode: 'tree', permissionActionCode: 'view', title: 'Tree', authorized: true },
      { actionCode: 'disable', permissionActionCode: 'enable', title: 'Disable', authorized: true },
    ],
  };
}

test('normalizeError keeps AppError and wraps unknown errors', () => {
  const appError = new AppError('conflict', { code: platformErrorCodes.conflictVersion, status: 409 });

  assert.equal(normalizeError(appError), appError);
  assert.deepEqual(
    normalizeError(new Error('boom')),
    new AppError('boom', { code: platformErrorCodes.appError }),
  );
  assert.deepEqual(normalizeError('boom').details, { cause: 'boom' });
});

test('resolveGlobalErrorPresentation maps common failures to fixed global slots', () => {
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('login required', { status: 401 }), {
      phase: 'action',
      surface: 'workbench',
    }).slot,
    'redirect-login',
  );
  assert.equal(
    resolveGlobalErrorPresentation(
      new AppError('bad credentials', { code: platformErrorCodes.loginBadCredentials, status: 401 }),
      {
        phase: 'action',
        surface: 'form',
      },
    ).slot,
    'global-toast',
  );
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('forbidden', { status: 403 }), {
      phase: 'page-load',
      surface: 'workbench',
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
      surface: 'workbench',
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
  assert.equal(
    resolveGlobalErrorPresentation(new AppError('bad request', { status: 400 })).slot,
    'global-toast',
  );
});
