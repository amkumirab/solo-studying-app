# ADR-001: Version the planner document without changing study credit

## Status

Accepted for the first university planner implementation slice.

## Date

2026-10-16

## Context

The existing planner stores one flat version-1 JSON record in private preferences.
Future weekly planning needs richer data, but current editing and rebuild paths
only understand date-only blocks. Adding timing or lock fields before updating
those paths would allow edits to discard user choices. Block IDs also determine
the allocation of recorded study credit; a migration must not regenerate them.

## Decision

Separate the JSON codec from persistence. New saves use a version-2 envelope with
`version`, `kind` (`date_only`), and `plan`. The plan body retains the existing
`start`, `sessionMinutes`, `capacity`, `courses`, and `blocks` fields.

Read version 1 into the same domain model without modifying the stored record.
Write version 2 only on explicit save. Preserve every course and block field,
list order, shortfall, and study-credit baseline. Do not invent clock times,
renumber IDs, remap priorities, or modify course goals and study history.

Both versions require the exact documented field sets, integral numeric values,
ISO dates, and the existing domain limits. Records are capped at 2,000,000
characters. Reject unknown fields, document kinds, and versions rather than
silently dropping behavior-bearing information. Future schema additions require
a version bump and a deliberate compatibility reader.

The store refuses saves over malformed or unsupported existing records. The
existing confirmed Clear action is the explicit recovery path. Store mutations
share a process-wide lock so separate store instances cannot interleave a
validation-and-save with another store mutation. This is not a cross-process
transaction or a replacement for the operating system's persistence guarantees.

## Alternatives considered

- Add all future metadata now: rejected because current editing paths cannot
  preserve its meaning safely.
- Rewrite on read: rejected because merely opening the planner should not mutate
  the user's only saved record.
- Replace preferences with a database: deferred; the current single-record size
  and access pattern do not require a new storage backend for this slice.

## Acceptance checks

- A checked-in version-1 fixture reopens without changing its stored bytes.
- Explicit save upgrades it to version 2 with equal domain data and progress,
  including sparse IDs, moved dates and fractional recorded minutes.
- New documents round-trip deterministically; empty block lists remain valid.
- Malformed, oversized, mistyped, unknown-field and unsupported records are
  rejected and preserved until explicitly cleared.
- Scheduling, UI, lint, and APK build checks continue to pass.

## Consequences

The codec is an explicit compatibility boundary for later roadmap slices. This
commit adds no new planner screens, timed sessions, weekly goals or lock policy.
Opening and saving on a physical device remains a manual release check.
