import { notification } from 'ant-design-vue';
import { h } from 'vue';

export type UiFeedbackTone = 'error' | 'success';

export interface UiFeedbackOptions {
  tone: UiFeedbackTone;
  content: string;
}

const DEFAULT_DURATION_SECONDS = 2.6;

export function showFeedback(options: UiFeedbackOptions) {
  if (typeof document === 'undefined') {
    return;
  }
  if (options.tone === 'error') {
    notification.error({
      message: () => feedbackContent(options.content, options.tone),
      duration: DEFAULT_DURATION_SECONDS,
      placement: 'top',
      class: `muyun-feedback-notification muyun-feedback-notification-${options.tone}`,
      style: compactFeedbackStyle,
    });
    return;
  }
  notification.success({
    message: () => feedbackContent(options.content, options.tone),
    duration: DEFAULT_DURATION_SECONDS,
    placement: 'topRight',
    class: `muyun-feedback-notification muyun-feedback-notification-${options.tone}`,
    style: compactFeedbackStyle,
  });
}

const compactFeedbackStyle = {
  width: 'fit-content',
  maxWidth: 'calc(100vw - 40px)',
  padding: '9px 12px',
  marginBottom: '8px',
};

export function showErrorMessage(content: string) {
  showFeedback({ tone: 'error', content });
}

export function showSuccessMessage(content: string) {
  showFeedback({ tone: 'success', content });
}

function feedbackContent(content: string, tone: UiFeedbackTone) {
  return h('span', { class: `muyun-feedback-content muyun-feedback-${tone}` }, [
    h('span', { class: 'muyun-feedback-text' }, content),
    h('span', {
      class: 'muyun-feedback-timebar',
      'aria-hidden': 'true',
      style: { '--muyun-feedback-duration': `${DEFAULT_DURATION_SECONDS}s` },
    }),
  ]);
}
