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
