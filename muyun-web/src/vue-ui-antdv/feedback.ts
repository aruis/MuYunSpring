import { message } from 'ant-design-vue';

export function showErrorMessage(content: string) {
  if (typeof document === 'undefined') {
    return;
  }
  void message.error(content);
}
