import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createPlatformActionResultEffectHandlers,
  handlePlatformActionSuccess,
  platformActionResultEffects,
  platformActionResultEffectTypes,
  resolvePlatformActionResult,
  resolvePlatformActionResultMessage,
  withPlatformActionResultEffects,
} from '../src/platform-components/platformActionResultFeedback.ts';
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

test('platform action result message prefers business message and falls back safely', () => {
  assert.equal(resolvePlatformActionResultMessage({ message: '已保存' }), '已保存');
  assert.equal(resolvePlatformActionResultMessage({ message: '   ' }, '默认成功'), '默认成功');
  assert.equal(resolvePlatformActionResultMessage({ count: 1 }, '已删除'), '已删除');
  assert.equal(resolvePlatformActionResultMessage(undefined), '操作成功');
});

test('platform action result resolves structured facts without UI coupling', async () => {
  const actionResult = resolvePlatformActionResult({
    message: '已刷新',
    resultType: 'updated',
    effects: [
      { type: 'refresh-list', payload: { moduleAlias: 'iam.employee' } },
      { type: '' },
      'refresh-detail',
      { type: 'close-editor', payload: [] },
    ],
  });

  assert.equal(actionResult.message, '已刷新');
  assert.equal(actionResult.resultType, 'updated');
  assert.deepEqual(actionResult.effects, [
    { type: 'refresh-list', payload: { moduleAlias: 'iam.employee' } },
  ]);

  const handled: string[] = [];
  await handlePlatformActionSuccess(actionResult.raw, {
    effectHandlers: {
      'refresh-list': (effect, result) => {
        handled.push(`${effect.type}:${result.resultType}`);
      },
    },
  });
  assert.deepEqual(handled, ['refresh-list:updated']);
});

test('platform action result standard effects compose without duplicate local defaults', async () => {
  const result = withPlatformActionResultEffects(
    {
      message: '已处理',
      effects: [platformActionResultEffects.refreshList({ source: 'backend' })],
    },
    [
      platformActionResultEffects.refreshList({ source: 'local' }),
      platformActionResultEffects.closeEditor(),
    ],
  );

  assert.deepEqual(resolvePlatformActionResult(result).effects, [
    { type: platformActionResultEffectTypes.refreshList, payload: { source: 'backend' } },
    { type: platformActionResultEffectTypes.closeEditor },
  ]);

  const handled: string[] = [];
  await handlePlatformActionSuccess(result, {
    effectHandlers: createPlatformActionResultEffectHandlers({
      refreshList: (effect) => {
        handled.push(`${effect.type}:${effect.payload?.source}`);
      },
      closeEditor: (effect) => {
        handled.push(effect.type);
      },
    }),
  });

  assert.deepEqual(handled, ['refresh-list:backend', 'close-editor']);
});
