export const moduleAbilityCodes = {
  crud: 'crud',
  softDelete: 'softDelete',
  lifecycle: 'lifecycle',
  cache: 'cache',
  tree: 'tree',
  sort: 'sort',
  reference: 'reference',
  enable: 'enable',
  dataScope: 'dataScope',
  workflow: 'workflow',
  approval: 'approval',
  childRelation: 'childRelation',
  referenceDependency: 'referenceDependency',
  exchange: 'exchange',
} as const;

export type ModuleAbilityCode = (typeof moduleAbilityCodes)[keyof typeof moduleAbilityCodes];
