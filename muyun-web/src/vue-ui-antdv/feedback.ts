type FeedbackTone = 'error' | 'success';

const FEEDBACK_STYLE_ID = 'muyun-global-feedback-style';
const DEFAULT_DURATION_MS = 2600;

export function showErrorMessage(content: string) {
  showGlobalFeedback(content, 'error');
}

export function showSuccessMessage(content: string) {
  showGlobalFeedback(content, 'success');
}

function showGlobalFeedback(content: string, tone: FeedbackTone) {
  if (typeof document === 'undefined') {
    return;
  }
  ensureGlobalFeedbackStyle();
  const container = ensureFeedbackContainer(tone);
  const item = document.createElement('div');
  item.className = `muyun-global-feedback-item ${tone}`;
  item.setAttribute('role', tone === 'error' ? 'alert' : 'status');
  item.innerHTML = `
    <span class="muyun-global-feedback-icon">${tone === 'error' ? '!' : 'OK'}</span>
    <span class="muyun-global-feedback-content"></span>
  `;
  item.querySelector('.muyun-global-feedback-content')!.textContent = content;
  container.append(item);
  window.setTimeout(() => {
    item.classList.add('leaving');
    window.setTimeout(() => item.remove(), 180);
  }, DEFAULT_DURATION_MS);
}

function ensureFeedbackContainer(tone: FeedbackTone) {
  const id = `muyun-global-feedback-${tone}`;
  const existing = document.getElementById(id);
  if (existing) {
    return existing;
  }
  const container = document.createElement('div');
  container.id = id;
  container.className = `muyun-global-feedback ${tone}`;
  document.body.append(container);
  return container;
}

function ensureGlobalFeedbackStyle() {
  if (document.getElementById(FEEDBACK_STYLE_ID)) {
    return;
  }
  const style = document.createElement('style');
  style.id = FEEDBACK_STYLE_ID;
  style.textContent = `
    .muyun-global-feedback {
      position: fixed;
      z-index: 3000;
      display: grid;
      gap: 8px;
      pointer-events: none;
    }

    .muyun-global-feedback.success {
      top: 18px;
      right: 20px;
      justify-items: end;
    }

    .muyun-global-feedback.error {
      top: 18px;
      left: 50%;
      justify-items: center;
      transform: translateX(-50%);
    }

    .muyun-global-feedback-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      max-width: min(520px, calc(100vw - 40px));
      padding: 9px 12px;
      border: 1px solid var(--muyun-border);
      border-radius: 8px;
      background: var(--muyun-surface);
      box-shadow: 0 10px 28px rgb(15 23 42 / 16%);
      color: var(--muyun-text);
      font-size: 13px;
      line-height: 1.4;
      pointer-events: auto;
      transition:
        opacity 0.18s ease,
        transform 0.18s ease;
    }

    .muyun-global-feedback-item.leaving {
      opacity: 0;
      transform: translateY(-6px);
    }

    .muyun-global-feedback-item.success {
      border-color: var(--muyun-success-border);
    }

    .muyun-global-feedback-item.error {
      border-color: var(--muyun-danger-border);
    }

    .muyun-global-feedback-icon {
      display: inline-grid;
      flex: 0 0 auto;
      place-items: center;
      width: 20px;
      height: 18px;
      border-radius: 999px;
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      line-height: 1;
    }

    .muyun-global-feedback-item.success .muyun-global-feedback-icon {
      background: var(--muyun-success-text);
    }

    .muyun-global-feedback-item.error .muyun-global-feedback-icon {
      background: var(--muyun-danger-text);
    }
  `;
  document.head.append(style);
}
