# ADR-003: Store local availability without inventing timed sessions

## Status and date

Accepted for the availability roadmap slice, 2026-10-18.

## Context

The planner currently stores date-only blocks. Weekly clock windows are needed
before a timed scheduling engine can be introduced. Replacing the existing
document now would risk block IDs, progress credit and manually moved dates.

## Decision

Add a separate version-1 availability record alongside the plan and subject
catalog in private preferences. Extend the atomic expected-snapshot guard to all
three records. Write only changed records, reject unsupported data and offer
record-specific confirmed recovery. Reads never backfill or migrate preferences.

Represent local windows as integer minutes, with inclusive start and exclusive
end. Permit 00:00 starts and 24:00 ends, half-hour increments and adjacent windows.
Reject overnight/overlapping windows. Keep the existing 480-minute daily capacity
limit and bound each day to 16 windows. Disabled days retain windows but add no
capacity; an all-rest week is valid configuration, not a schedulable plan.

Saved windows supply daily capacity only when editing or confirming a rebuild.
The budget comparison may reduce that capacity but cannot exceed it. Preserve
all legacy behavior when no availability has been saved. Individual date-only
blocks do not acquire clock times, and no elapsed-time or DST claim is made.

## Alternatives and consequences

- Adding times to the plan schema now couples settings to an unfinished engine;
  defer timed blocks and their migration to the scheduling/calendar slice.
- Rewriting old daily budgets as clock windows invents preferences. Suggest
  an explicitly unsaved, half-hour-rounded draft instead.
- Automatic relocation on settings changes would alter accepted work without
  review. Keep explicit editing/rebuilding until timed regeneration is supported.
- No database migration, dependency, permission or network service is required.

## Verification

Test strict JSON and bounds, adjacency, overlap, midnight, disabled days and exact
totals. Preserve literal legacy record bytes, study-credit IDs and baselines.
Exercise stale-save retries, separate recovery, editor restoration, explicit
capacity application and budget ceilings. Run the full suite, lint and APK builds.
