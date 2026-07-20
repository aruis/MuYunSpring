export type WorkbenchRealtimeStatus = 'unavailable' | 'connecting' | 'connected' | 'disconnected';

export interface WorkbenchRealtimeStatusPresentation {
  label: string;
  title: string;
  tone: Exclude<WorkbenchRealtimeStatus, 'unavailable'>;
}

export function presentWorkbenchRealtimeStatus(
  status: WorkbenchRealtimeStatus,
): WorkbenchRealtimeStatusPresentation | undefined {
  if (status === 'connected') {
    return { label: '实时连接正常', title: '与平台实时服务连接正常', tone: status };
  }
  if (status === 'connecting') {
    return { label: '实时连接中', title: '正在连接平台实时服务', tone: status };
  }
  if (status === 'disconnected') {
    return { label: '实时连接已断开', title: '与平台实时服务的连接已断开，正在等待恢复', tone: status };
  }
  return undefined;
}
