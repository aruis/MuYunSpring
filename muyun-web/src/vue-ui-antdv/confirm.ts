import { Input, Modal } from 'ant-design-vue';
import { h } from 'vue';
import type { UiConfirmOptions } from './types';

export const confirmAction = createConfirmAction(Modal.confirm);

export function createConfirmAction(confirm: typeof Modal.confirm) {
  return function confirmAction(options: UiConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      const requiredText = options.requiredText;
      let typedText = '';
      const modal = confirm({
        title: options.title,
        content: requiredText
          ? () =>
              h('div', [
                options.content ? h('p', options.content) : undefined,
                h('p', `请输入「${requiredText}」以确认此操作。`),
                h(Input, {
                  value: typedText,
                  placeholder: requiredText,
                  autofocus: true,
                  'onUpdate:value': (value: string) => {
                    typedText = value;
                    modal.update({
                      okButtonProps: {
                        danger: options.danger,
                        disabled: !matchesRequiredText(requiredText, typedText),
                      },
                    });
                  },
                }),
              ])
          : options.content,
        okText: options.okText ?? '确认',
        cancelText: options.cancelText ?? '取消',
        okButtonProps: {
          danger: options.danger,
          disabled: Boolean(requiredText),
        },
        onOk: () => {
          if (!matchesRequiredText(requiredText, typedText)) {
            return Promise.reject(new Error('Confirmation text does not match'));
          }
          resolve(true);
        },
        onCancel: () => resolve(false),
      });
    });
  };
}

export function matchesRequiredText(requiredText: string | undefined, typedText: string) {
  return requiredText === undefined || typedText === requiredText;
}
