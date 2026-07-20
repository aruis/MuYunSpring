export interface DropdownTriggerRect {
  top: number;
  right: number;
  bottom: number;
  left: number;
  width: number;
}

export interface DropdownPopupLayoutInput {
  trigger: DropdownTriggerRect;
  popupWidth: number;
  popupHeight: number;
  viewportWidth: number;
  viewportHeight: number;
  align: 'start' | 'end';
  gap?: number;
  margin?: number;
  minimumWidth?: number;
}

export interface DropdownPopupLayout {
  top: number;
  left: number;
  minWidth: number;
  maxWidth: number;
  maxHeight: number;
  placement: 'top' | 'bottom';
}

export function resolveDropdownPopupLayout(input: DropdownPopupLayoutInput): DropdownPopupLayout {
  const gap = input.gap ?? 6;
  const margin = input.margin ?? 8;
  const viewportWidth = Math.max(0, input.viewportWidth);
  const viewportHeight = Math.max(0, input.viewportHeight);
  const maxWidth = Math.max(0, viewportWidth - margin * 2);
  const minWidth = Math.min(Math.max(input.trigger.width, input.minimumWidth ?? 112), maxWidth);
  const renderedWidth = Math.min(Math.max(input.popupWidth, minWidth), maxWidth);
  const requestedLeft = input.align === 'start' ? input.trigger.left : input.trigger.right - renderedWidth;
  const maximumLeft = Math.max(margin, viewportWidth - renderedWidth - margin);
  const left = clamp(requestedLeft, margin, maximumLeft);

  const bottomTop = input.trigger.bottom + gap;
  const spaceBelow = Math.max(0, viewportHeight - margin - bottomTop);
  const spaceAbove = Math.max(0, input.trigger.top - gap - margin);
  const placement = input.popupHeight > spaceBelow && spaceAbove > spaceBelow ? 'top' : 'bottom';
  const maxHeight = placement === 'top' ? spaceAbove : spaceBelow;
  const renderedHeight = Math.min(input.popupHeight, maxHeight);
  const top = placement === 'top' ? Math.max(margin, input.trigger.top - gap - renderedHeight) : bottomTop;

  return { top, left, minWidth, maxWidth, maxHeight, placement };
}

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(Math.max(value, minimum), maximum);
}
