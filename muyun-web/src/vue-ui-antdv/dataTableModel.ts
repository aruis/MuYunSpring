export interface UiDataTableScrollOptions {
  horizontal: boolean;
  fillHeight: boolean;
  hasFixedColumn: boolean;
}

export interface UiDataTableResolvedScroll {
  x?: 'max-content';
  y?: '100%';
}

export function resolveUiDataTableScroll(
  options: UiDataTableScrollOptions,
): UiDataTableResolvedScroll | undefined {
  const scroll: UiDataTableResolvedScroll = {};
  if (options.horizontal || options.hasFixedColumn) {
    scroll.x = 'max-content';
  }
  if (options.fillHeight) {
    scroll.y = '100%';
  }
  return Object.keys(scroll).length > 0 ? scroll : undefined;
}
