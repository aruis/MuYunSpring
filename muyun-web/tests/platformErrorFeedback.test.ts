import test from 'node:test';
import assert from 'node:assert/strict';
import { matchesPlatformActionErrorHandler } from '../src/platform-components/platformErrorFeedback.ts';
import { AppError, platformErrorCodes } from '../src/web-core/index.ts';

test('platform action error handler matches by code or marker facts', () => {
  const codedError = new AppError('resource conflict', {
    code: platformErrorCodes.resourceInUse,
    details: { marker: 'dictionaryCategory' },
  });
  const reasonError = new AppError('position is referenced by employees', {
    code: platformErrorCodes.internalError,
    details: { reason: 'position' },
  });
  const errorKeyError = new AppError('employee is referenced by accounts', {
    details: { errorKey: 'employee' },
  });
  const messageMarkerError = new AppError('dictionaryCategory still exists');

  assert.equal(
    matchesPlatformActionErrorHandler(codedError, {
      code: platformErrorCodes.resourceInUse,
      handle: () => undefined,
    }),
    true,
  );
  assert.equal(
    matchesPlatformActionErrorHandler(codedError, {
      marker: 'dictionaryCategory',
      handle: () => undefined,
    }),
    true,
  );
  assert.equal(
    matchesPlatformActionErrorHandler(reasonError, {
      marker: 'position',
      handle: () => undefined,
    }),
    true,
  );
  assert.equal(
    matchesPlatformActionErrorHandler(errorKeyError, {
      marker: 'employee',
      handle: () => undefined,
    }),
    true,
  );
  assert.equal(
    matchesPlatformActionErrorHandler(messageMarkerError, {
      marker: 'dictionaryCategory',
      handle: () => undefined,
    }),
    true,
  );
  assert.equal(
    matchesPlatformActionErrorHandler(codedError, {
      marker: 'employee',
      handle: () => undefined,
    }),
    false,
  );
});
