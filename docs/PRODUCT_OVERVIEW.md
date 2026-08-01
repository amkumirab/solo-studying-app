# Product overview

## Purpose

Solo Studying helps a learner turn long goals into short, trackable focus
sessions. The RPG vocabulary is presentation: progress still comes from time
spent studying and actions explicitly confirmed by the user.

## Core loop

1. Create a dungeon for a subject or long-running goal.
2. Add a boss with an estimated number of focus minutes.
3. Select an optional skill and start a timed session.
4. Apply the completed time to boss health and skill progress.
5. Award experience and gold.
6. Spend gold on a user-defined real-life reward.

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

Daily alarms trigger local reminders. Message selection uses current progress,
study-day configuration, streak state, and active bosses. Notification failures
must not affect stored study progress.

### Audio

Short feedback sounds are synthesized on-device with `AudioTrack`. The app does
not bundle third-party music or sound effects.

## Design boundaries

- The application must remain useful without a network connection.
- A timer interruption must not silently award progress.
- Real-world achievements require explicit confirmation.
- Missed days should create a recovery path, not erase historical progress.
- User-created goals and rewards remain private unless an export feature is
  deliberately used.

## Planned work

- Break large Compose files into feature-focused components.
- Let users edit notification times and disable individual reminders.
- Add JSON export and restore with schema versioning.
- Add accessibility checks and end-to-end UI tests.
- Add a migration test for every future database version.

