# platform-components

`platform-components` is reserved for reusable platform-level business components.

Typical examples:

- reference selectors
- dictionary-aware controls
- attachment panels
- import/export action groups
- workflow action panels
- query tables and child tables

This layer may compose `vue-ui-antdv`, `web-core`, `dynamic-page-runtime`, and `web-contracts`, but should only accept business semantics that are stable across multiple business projects.

## Management State Helpers

`useFlatCrudManagementState` is for single-record management pages where the detail area is the primary workspace. Empty states may stay in `create` mode so the page can guide users to create the first record.

`createRecordEditorSessionState` is for local editor sessions inside composite management pages. Canceling a create session closes the editor by returning to `view`; canceling an edit session restores the selected record draft.

By default, `startCreate()` clears the selected record. Use `preserveSelection` when the selected record is the surrounding context, and use `selectedRecord` plus a custom `draft`/`mode` when creating a child record under a parent.
