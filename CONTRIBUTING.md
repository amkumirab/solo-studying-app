# Contributing

Keep changes focused and describe the user-visible behavior in the commit or
pull request. Before submitting a change:

1. Run `./gradlew testDebugUnitTest lintDebug`.
2. Add a test when changing progression, rewards, schedules, or persistence.
3. Keep database changes paired with a Room migration.
4. Keep UI state in a view model instead of storing it in activity fields.
5. Do not commit signing keys, local SDK paths, generated build output, or user
   study data.

Use imperative commit subjects, for example:

```text
feat: add editable reminder times
fix: preserve boss progress after timer pause
test: cover failed reward purchase
```

