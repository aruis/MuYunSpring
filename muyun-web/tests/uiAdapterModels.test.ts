import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveUiDataTableScroll } from '../src/vue-ui-antdv/dataTableModel.ts';

test('data table enables horizontal scrolling independently from fixed columns', () => {
  assert.deepEqual(resolveUiDataTableScroll({ horizontal: true, fillHeight: false, hasFixedColumn: false }), {
    x: 'max-content',
  });
  assert.deepEqual(resolveUiDataTableScroll({ horizontal: false, fillHeight: false, hasFixedColumn: true }), {
    x: 'max-content',
  });
  assert.equal(
    resolveUiDataTableScroll({ horizontal: false, fillHeight: false, hasFixedColumn: false }),
    undefined,
  );
});

test('data table combines height filling with horizontal scrolling', () => {
  assert.deepEqual(resolveUiDataTableScroll({ horizontal: true, fillHeight: true, hasFixedColumn: false }), {
    x: 'max-content',
    y: '100%',
  });
});
