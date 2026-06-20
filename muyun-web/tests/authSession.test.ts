import test from 'node:test';
import assert from 'node:assert/strict';
import { AppError } from '../src/web-core/index.ts';
import { effectiveAuthToken, isAuthenticationRequiredError } from '../src/app/authSession.ts';

test('effectiveAuthToken falls back to env token outside browser storage', () => {
  assert.equal(effectiveAuthToken(' env-token '), 'env-token');
});

test('effectiveAuthToken ignores blank env token', () => {
  assert.equal(effectiveAuthToken('   '), undefined);
});

test('isAuthenticationRequiredError uses backend auth-required code for login recovery', () => {
  assert.equal(
    isAuthenticationRequiredError(new AppError('login required', { code: 'AUTHENTICATION_REQUIRED', status: 401 })),
    true,
  );
  assert.equal(
    isAuthenticationRequiredError(new AppError('bad credentials', { code: 'AUTHENTICATION_FAILED', status: 401 })),
    false,
  );
  assert.equal(
    isAuthenticationRequiredError(new AppError('legacy login required', { code: 'HTTP_ERROR', status: 401 })),
    true,
  );
  assert.equal(isAuthenticationRequiredError(new AppError('forbidden', { status: 403 })), false);
  assert.equal(isAuthenticationRequiredError(new AppError('menu scheme missing', { status: 409 })), false);
});
