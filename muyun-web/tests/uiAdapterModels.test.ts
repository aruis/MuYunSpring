import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveUiDataTableScroll } from '../src/vue-ui-antdv/dataTableModel.ts';
import { resolveDropdownPopupLayout } from '../src/vue-ui-antdv/dropdownPosition.ts';

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

test('dropdown flips above a bottom trigger and keeps viewport margin', () => {
  const layout = resolveDropdownPopupLayout({
    trigger: { top: 740, right: 300, bottom: 770, left: 260, width: 40 },
    popupWidth: 160,
    popupHeight: 140,
    viewportWidth: 1024,
    viewportHeight: 800,
    align: 'end',
  });

  assert.equal(layout.placement, 'top');
  assert.equal(layout.top, 594);
  assert.equal(layout.maxHeight, 726);
});

test('dropdown clamps start alignment at the right viewport edge', () => {
  const layout = resolveDropdownPopupLayout({
    trigger: { top: 40, right: 310, bottom: 70, left: 290, width: 20 },
    popupWidth: 180,
    popupHeight: 80,
    viewportWidth: 320,
    viewportHeight: 240,
    align: 'start',
  });

  assert.equal(layout.left, 132);
  assert.equal(layout.maxWidth, 304);
});

test('dropdown constrains height when neither side fits the popup', () => {
  const layout = resolveDropdownPopupLayout({
    trigger: { top: 100, right: 160, bottom: 130, left: 120, width: 40 },
    popupWidth: 160,
    popupHeight: 300,
    viewportWidth: 320,
    viewportHeight: 200,
    align: 'end',
  });

  assert.equal(layout.placement, 'top');
  assert.equal(layout.top, 8);
  assert.equal(layout.maxHeight, 86);
});
