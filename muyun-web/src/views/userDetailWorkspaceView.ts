import { defineAsyncComponent } from 'vue';
import { defineWorkspaceView } from '../platform-admin-runtime/workspaceViewContract';

export interface UserDetailWorkspaceViewInput {
  recordId: string;
}

export const userDetailWorkspaceView = defineWorkspaceView<UserDetailWorkspaceViewInput>({
  type: 'iam.user.detail',
  route: '/iam/users',
  moduleAlias: 'iam.user',
  component: defineAsyncComponent(() => import('./UserDetailWorkspaceView.vue')),
  layout: 'workspace',
  routeTitle: '用户管理',
  presentations: ['drawer', 'tab'],
  titleOf: () => '用户详情',
  parse(query) {
    const recordId = query.recordId;
    if (typeof recordId !== 'string' || !recordId) {
      return undefined;
    }
    return { recordId };
  },
});
