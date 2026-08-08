export interface PlatformFileSizeDisplay {
  text: string;
  title: string;
  valid: boolean;
}

export interface PlatformFileSizeOptions {
  emptyText?: string;
  locale?: string;
}

const UNITS = ['B', 'KB', 'MB', 'GB'] as const;
const UNIT_BASE = 1024;

export function formatPlatformFileSize(
  value: number | string | bigint | null | undefined,
  options: PlatformFileSizeOptions = {},
): PlatformFileSizeDisplay {
  const emptyText = options.emptyText ?? '-';
  const bytes = byteCount(value);
  if (bytes === undefined) {
    return { text: emptyText, title: emptyText, valid: false };
  }

  const numericBytes = Number(bytes);
  let unitIndex = 0;
  while (unitIndex < UNITS.length - 1 && numericBytes >= UNIT_BASE ** (unitIndex + 1)) {
    unitIndex += 1;
  }
  const scaled = numericBytes / UNIT_BASE ** unitIndex;
  const locale = options.locale ?? 'zh-CN';
  const text = `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(scaled)} ${UNITS[unitIndex]}`;
  const exactBytes = new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(bytes);
  return { text, title: `${exactBytes} bytes`, valid: true };
}

function byteCount(value: number | string | bigint | null | undefined): number | bigint | undefined {
  if (typeof value === 'bigint') {
    return value >= 0n ? value : undefined;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) && Number.isInteger(value) && value >= 0 ? value : undefined;
  }
  if (typeof value === 'string' && /^\d+$/.test(value.trim())) {
    return BigInt(value.trim());
  }
  return undefined;
}
