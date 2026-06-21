import test from 'node:test';
import assert from 'node:assert/strict';
import { createAuthClient, createHttpClient } from '../src/web-core/index.ts';

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
