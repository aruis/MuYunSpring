import test from 'node:test';
import assert from 'node:assert/strict';
import { Modal } from 'ant-design-vue';
import type { VNode } from 'vue';
import { createConfirmAction, matchesRequiredText } from '../src/vue-ui-antdv/confirm.ts';

test('required confirmation text enables destructive confirmation only after an exact match', async () => {
  let options!: Parameters<typeof Modal.confirm>[0];
  const updates: Parameters<typeof Modal.confirm>[0][] = [];
  const confirm = createConfirmAction((nextOptions) => {
    options = nextOptions;
    return {
      destroy() {},
      update(update) {
        updates.push(update as Parameters<typeof Modal.confirm>[0]);
      },
    };
  });

  const confirming = confirm({ title: '删除租户', requiredText: 'demo', danger: true });
  assert.equal(options.okButtonProps?.disabled, true);

  const content = (options.content as () => VNode)();
  const input = (content.children as VNode[])[2];
  const updateValue = input.props?.['onUpdate:value'] as (value: string) => void;
  updateValue('demo ');
  updateValue('demo');

  assert.deepEqual(updates, [
    { okButtonProps: { danger: true, disabled: true } },
    { okButtonProps: { danger: true, disabled: false } },
  ]);
  options.onOk?.();
  assert.equal(await confirming, true);
});

test('required confirmation text rejects mismatches and cancellation resolves false', async () => {
  assert.equal(matchesRequiredText('demo', 'demo'), true);
  assert.equal(matchesRequiredText('demo', 'demo '), false);
  assert.equal(matchesRequiredText(undefined, ''), true);

  let options!: Parameters<typeof Modal.confirm>[0];
  const confirm = createConfirmAction((nextOptions) => {
    options = nextOptions;
    return { destroy() {}, update() {} };
  });
  const confirming = confirm({ title: '删除租户', requiredText: 'demo' });

  options.onCancel?.();
  assert.equal(await confirming, false);
});
