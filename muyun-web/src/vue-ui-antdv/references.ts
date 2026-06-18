import type { Primitive } from '@muyun/web-contracts';

export interface ReferenceRecord {
  key: string | number;
  title: string;
  subtitle?: string;
  fields: Record<string, Primitive>;
}

const references: Record<string, ReferenceRecord[]> = {
  'crm.customer': [
    {
      key: 'cust-001',
      title: '云舟科技',
      subtitle: '软件服务 / 战略客户',
      fields: {
        customerId: 'cust-001',
        customerName: '云舟科技',
        industry: 'software',
        level: 'strategic',
      },
    },
    {
      key: 'cust-002',
      title: '衡岳制造',
      subtitle: '制造业 / 重点客户',
      fields: {
        customerId: 'cust-002',
        customerName: '衡岳制造',
        industry: 'manufacturing',
        level: 'key',
      },
    },
  ],
};

export async function searchReferenceRecords(moduleAlias: string, keyword: string) {
  await new Promise((resolve) => window.setTimeout(resolve, 120));
  const normalized = keyword.trim().toLowerCase();
  const records = references[moduleAlias] ?? [];
  if (!normalized) {
    return records;
  }
  return records.filter((record) =>
    `${record.title} ${record.subtitle ?? ''}`.toLowerCase().includes(normalized),
  );
}
