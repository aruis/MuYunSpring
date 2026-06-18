# web-core

`web-core` is reserved for non-visual platform capabilities shared by platform and business web projects.

Typical responsibilities:

- HTTP client and request context
- normalized errors and user-facing messages
- tenant, user, permission, and trace context helpers
- query defaults, cache keys, and invalidation helpers
- lightweight application events and observability hooks

This layer should stay UI-library independent.
