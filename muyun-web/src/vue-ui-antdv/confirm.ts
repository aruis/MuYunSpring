import { Modal } from 'ant-design-vue';
import type { UiConfirmOptions } from './types';

export function confirmAction(options: UiConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: options.title,
      content: options.content,
      okText: options.okText ?? '确认',
      cancelText: options.cancelText ?? '取消',
      okButtonProps: options.danger ? { danger: true } : undefined,
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    });
  });
}
