import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceViewDescriptor } from '../src/app/workspaceViews.ts';
import { moduleActionManagementWorkspaceView } from '../src/views/moduleActionManagementWorkspaceView.ts';

test('creates one closable action-management tab per governed module', () => {
  const descriptor = createWorkspaceViewDescriptor(moduleActionManagementWorkspaceView, {
    moduleAlias: 'education.teacher',
    moduleTitle: '教师',
  });

  assert.equal(descriptor.title, '动作：教师');
  assert.equal(descriptor.layout, 'workspace');
  assert.equal(descriptor.target.route, '/config/modules');
  assert.equal(descriptor.target.moduleAlias, 'platform.module');
  assert.deepEqual(descriptor.target.query, {
    workspaceView: 'platform.module.actions',
    workspacePresentation: 'tab',
    moduleAlias: 'education.teacher',
    moduleTitle: '教师',
  });
  assert.equal(descriptor.tabPolicy.identity, 'by-params');
});
