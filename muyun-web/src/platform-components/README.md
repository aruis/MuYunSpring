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
