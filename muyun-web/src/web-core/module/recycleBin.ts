import type { ModuleContext } from './moduleContext';
import { moduleAbilityCodes } from './abilityCodes';

/**
 * 回收站是模块能力，查询权限只决定当前用户能否进入该能力。
 */
export function hasRecycleBinAbility<TRecord>(context: ModuleContext<TRecord>): boolean {
  return context.abilities.has(moduleAbilityCodes.recycleBin) === true;
}

export function canQueryRecycleBin<TRecord>(context: ModuleContext<TRecord>): boolean {
  return hasRecycleBinAbility(context) && context.can('recycleBinQuery') === true;
}
