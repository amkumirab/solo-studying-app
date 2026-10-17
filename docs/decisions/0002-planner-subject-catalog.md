# ADR-002: Keep subject preferences separate from accepted study credit

## Status and date

Accepted, 2026-10-17. Implements the subject-management roadmap slice only.

## Context

Subjects need editable names, priorities, colors, weekly preferences and optional
deadlines. Existing goal names need not be unique, total effort is not a weekly
target, and accepted block IDs determine progress credit. Editing metadata must
not silently regenerate sessions or discard recorded study.

## Decision

Store up to 20 validated subjects in a separate version-1 catalog within the
existing private planner preferences. Link each entry to a unique existing goal
ID. Subject names are planner-only aliases and must be unique within the catalog.
Use named theme colors rather than new assets or arbitrary unreadable text colors.

Plan version 3 expands priority bounds to 1–5. Continue reading versions 1 and 2
with their original 1–3 bounds and exact numeric values. Never backfill subjects
or rewrite plan bytes just by opening the planner or changing subject metadata.

Subject priority and deadline seed explicit editing or confirmed rebuilding.
Without an exam deadline, an accepted plan keeps its explicit finish date; a new
plan asks for a separate finish-by horizon. Do not inherit a goal deadline after
the user has cleared the subject deadline. Weekly preferences remain optional
metadata until weighted allocation is implemented in a later slice.

Confirmed subject deletion removes only its course and planned blocks. Keep
surviving IDs, order and baselines. Goals, recorded study and rewards are untouched.

Compare expected plan/catalog snapshots under the existing process-wide lock and
publish both records with one preferences commit. Bind plan saves to the snapshot
that produced the draft, not a refreshed snapshot on retry. After conflicts,
reload accepted state and invalidate stale editors and pending move dialogs.
Unsupported records require separate confirmed recovery actions for each record.
This is single-process coordination, not a cross-process database transaction.

## Alternatives and consequences

- Embedding aliases in every block duplicates mutable metadata and risks losing
  settings when blocks are rebuilt; the ID-linked catalog avoids that coupling.
- Reusing total remaining hours as a weekly target changes the meaning of old
  goals and can erase workload; keep the two concepts separate.
- Immediate rescheduling on metadata edits would relocate accepted work without
  review; retain explicit edit/rebuild confirmation in this date-only planner.
- A new database, network service or dependency is unnecessary for this slice.

## Verification

Cover strict schemas and bounds, unchanged legacy data, optional deadline clearing,
fractional weekly-minute preservation, confirmation and deletion credit, repeated
stale-save retries, guided defaults, linked-goal launch callbacks, recovery and
large-font layout. Dialog drafts use Android saved-state registries; screen-level
restoration tests do not substitute for physical Activity/process recreation.
