import test from 'node:test';
import assert from 'node:assert/strict';
import { AppError, platformErrorCodes } from '../src/web-core/index.ts';
import { effectiveAuthToken, isAuthenticationRequiredError } from '../src/app/authSession.ts';

test('effectiveAuthToken falls back to env token outside browser storage', () => {
  assert.equal(effectiveAuthToken(' env-token '), 'env-token');
});

test('effectiveAuthToken ignores blank env token', () => {
  assert.equal(effectiveAuthToken('   '), undefined);
});

test('isAuthenticationRequiredError uses backend auth-required code for login recovery', () => {
  assert.equal(
    isAuthenticationRequiredError(
      new AppError('login required', { code: platformErrorCodes.authRequired, status: 401 }),
    ),
    true,
  );
  assert.equal(
    isAuthenticationRequiredError(
      new AppError('token expired', { code: platformErrorCodes.authExpired, status: 401 }),
    ),
    true,
  );
  assert.equal(
    isAuthenticationRequiredError(
      new AppError('bad credentials', { code: platformErrorCodes.loginBadCredentials, status: 401 }),
    ),
    false,
  );
  assert.equal(
    isAuthenticationRequiredError(
      new AppError('legacy login required', { code: platformErrorCodes.httpError, status: 401 }),
    ),
    true,
  );
  assert.equal(isAuthenticationRequiredError(new AppError('forbidden', { status: 403 })), false);
  assert.equal(isAuthenticationRequiredError(new AppError('menu scheme missing', { status: 409 })), false);
});
