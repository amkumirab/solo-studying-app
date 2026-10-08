# Product overview

## Purpose

Solo Studying helps a learner turn long goals into short, trackable focus
sessions. The RPG vocabulary is presentation: progress still comes from time
spent studying and actions explicitly confirmed by the user.

## Core loop

1. Create a dungeon for a subject or long-running goal.
2. Add a boss with an estimated number of focus minutes.
3. Break large goals into timed study steps and focus on one clear action at a time.
4. Select an optional skill and start a timed session.
5. Apply the completed time to boss health and skill progress.
6. Award experience and gold.
7. Spend gold on a user-defined real-life reward.

The Today dashboard combines the daily time target, unfinished quests, upcoming
deadlines, and the next incomplete goal step into a short prioritized plan.

The home-screen widget surfaces the same daily progress and highest-priority
action outside the app. Its shortcut starts the user's saved quick-focus preset,
while tapping the rest of the widget opens the app normally.

A separate weekly streak widget shows whether each daily target was completed,
missed, scheduled later in the week, or configured as a rest day. It updates from
local session history and opens the profile statistics screen when selected.

Daily quests can be scheduled once, every day, or on selected weekdays. The app
creates only the current day's occurrence, preserves incomplete rollover items,
and uses a database uniqueness rule to prevent duplicate occurrences. A skipped
occurrence stays hidden for that date without disabling its future schedule.

## Implemented systems

### Progression

- One minute of focused time contributes to boss progress.
- Experience controls the profile level.
- Skills track time independently from the overall profile.
- Streak and recovery state are derived from the configured schedule.

### Bounded goal sessions

A boss's total study budget is separate from one focus session. Before starting
a goal, users can select 25, 45, or 60 minutes, or enter 1–480 minutes. A dungeon
plan suggests a session from its remaining daily target, capped at 60 minutes so
larger daily targets can be split into blocks with breaks.

The timer counts down the selected block while boss health reflects accumulated
progress against the whole goal. Sessions are capped at the remaining time for
ordinary unfinished goals. Deliverables can receive extra study after their
budget is reached and still require explicit completion confirmation.

Finishing a block records a completed study session and time-based rewards; it
does not defeat an unfinished boss. Reaching an ordinary goal's full budget
grants its boss reward once. Studying a completed goal preserves previous time
and grants only time-based rewards. Ending early records only actual elapsed
time. The bounded-session flag persists with the active timer, with a default
of false so sessions saved by earlier versions retain their original behavior.

### Offline exam planner

Study Hub > Today provides a guided planner for up to 20 existing course goals.
It asks for remaining focus hours, finish-by dates, priorities, daily capacity,
and preferred block length. The weekly plan protects earlier deadlines, prefers
full focus blocks, and reports effort that cannot fit. It uses deterministic
local rules without an online service or a new dependency.

Sessions start the existing bounded goal timer. Recorded time credits blocks in
their original order; moving an untouched session cannot move earned credit.
Rebuilding remaining work and removing a plan both require confirmation and
leave study history, goal targets, and rewards unchanged. Accepted plans live in
versioned local preferences. See [the planner contract](STUDY_PLANNER.md).

The Today dashboard includes the accepted plan's course sessions, with remaining
minutes and recorded progress. Matching course recommendations are hidden to
avoid duplicates. Long session lists expand on demand. "No time today" previews
moving unfinished sessions to later available days, preserving future blocks
and earned credit. A shortage prevents any partial save; confirmation is required.
See [the Today integration contract](TODAY_PLANNER.md).

### Persistence

Room stores the profile, dungeons, bosses, skills, rewards, reward balances, and
study sessions. The database is local to the device. Schema changes after a
public release must include explicit Room migrations.

### Notifications

Users can enable each daily reminder independently and choose its local time.
Alarms are restored after a reboot or clock change. Message selection uses
current progress, study-day configuration, streak state, and active bosses.
Notification failures must not affect stored study progress.

An active focus session appears as an ongoing notification with a system
countdown and pause, resume, and finish controls. Notification actions update
the same persisted session snapshot used by the in-app timer. A scheduled alarm
shows a completion alert if the app is in the background, while reopening the
app reconciles any elapsed time before progress and rewards are finalized.

### Audio

Short feedback sounds are preloaded and played with `SoundPool` to keep button,
session, warning, and progression feedback responsive. The bundled OGG files are
CC0 assets documented in `THIRD_PARTY_NOTICES.md`; the app does not include
audio extracted from films, television, anime, or games. Players can disable
interface audio or set a master volume; both preferences persist locally across
app restarts.

## Design boundaries

- The application must remain useful without a network connection.
- A timer interruption must not silently award progress.
- Real-world achievements require explicit confirmation.
- Missed days should create a recovery path, not erase historical progress.
- User-created goals and rewards remain private unless an export feature is
  deliberately used.

## Planned work

- Break large Compose files into feature-focused components.
- Add JSON export and restore with schema versioning.
- Add accessibility checks and end-to-end UI tests.
- Add a migration test for every future database version.
