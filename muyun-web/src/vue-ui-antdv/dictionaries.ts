import type { MuyunOption } from '@muyun/web-contracts';

const dictionaries: Record<string, MuyunOption[]> = {
  'crm.customer.level': [
    { label: '战略客户', value: 'strategic' },
    { label: '重点客户', value: 'key' },
    { label: '普通客户', value: 'normal' },
  ],
  'crm.industry': [
    { label: '制造', value: 'manufacturing' },
    { label: '软件服务', value: 'software' },
    { label: '物流', value: 'logistics' },
  ],
};

export function resolveDictionaryOptions(dictionaryAlias: string): MuyunOption[] {
  return dictionaries[dictionaryAlias] ?? [];
}
