import type { MuyunDynamicPageDescriptor } from '@muyun/web-contracts';

export const customerDescriptor: MuyunDynamicPageDescriptor = {
  moduleAlias: 'crm.customer',
  title: '客户档案',
  form: {
    fields: [
      {
        name: 'sourceCustomerId',
        label: '来源客户',
        kind: 'reference-select',
        placeholder: '搜索客户并回填',
        reference: {
          targetModuleAlias: 'crm.customer',
          keyField: 'customerId',
          labelField: 'customerName',
          fillBack: {
            customerId: 'customerId',
            customerName: 'customerName',
            industry: 'industry',
            level: 'level',
          },
        },
      },
      { name: 'customerName', label: '客户名称', kind: 'input', required: true },
      { name: 'industry', label: '行业', kind: 'dictionary-select', dictionaryAlias: 'crm.industry' },
      { name: 'level', label: '客户等级', kind: 'dictionary-select', dictionaryAlias: 'crm.customer.level' },
      {
        name: 'approvalReason',
        label: '战略客户说明',
        kind: 'input',
        visibleWhen: { field: 'level', equals: 'strategic' },
        requiredWhen: { field: 'level', equals: 'strategic' },
      },
    ],
  },
  list: {
    rowKey: 'id',
    columns: [
      { key: 'customerName', title: '客户名称' },
      { key: 'industry', title: '行业', dictionaryAlias: 'crm.industry' },
      { key: 'level', title: '等级', dictionaryAlias: 'crm.customer.level' },
    ],
  },
  actions: [
    { actionCode: 'save', title: '保存', level: 'primary', refresh: 'record' },
    { actionCode: 'submit', title: '提交审批', refresh: 'all' },
    { actionCode: 'export', title: '导出', disabled: true, disabledReason: '当前用户缺少导出权限' },
  ],
  initialRecord: {
    sourceCustomerId: null,
    customerId: null,
    customerName: '',
    industry: 'software',
    level: 'key',
    approvalReason: '',
  },
  records: [
    { id: 'cust-001', customerName: '云舟科技', industry: 'software', level: 'strategic' },
    { id: 'cust-002', customerName: '衡岳制造', industry: 'manufacturing', level: 'key' },
  ],
};
