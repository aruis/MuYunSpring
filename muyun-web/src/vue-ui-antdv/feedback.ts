import { message } from 'ant-design-vue';

export function showErrorMessage(content: string) {
  void message.error(content);
}
