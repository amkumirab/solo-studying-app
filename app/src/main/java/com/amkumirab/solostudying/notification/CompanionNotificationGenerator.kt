package com.amkumirab.solostudying.notification

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import java.util.Calendar
import java.util.Random

object CompanionNotificationGenerator {

    private val random = Random()

    // 1. Study Day Morning/Briefing Messages
    private val studyDayBriefings = listOf(
        "Your Hunter grows stronger with every study session.",
        "A Boss is waiting for you. Victory begins with one focused session.",
        "Every minute today brings you closer to defeating your Boss.",
        "The Dungeon awaits. Don't let today's opportunity disappear.",
        "Draw your focus blade, Hunter. Today's training session is critical.",
        "A legendary focus campaign is calling you. Settle in and prepare.",
        "Procrastination is a level 100 beast. Slay it with a single focused hour!"
    )

    // 2. Rest Day Relaxation Messages
    private val restDayMessages = listOf(
        "Today is your scheduled rest day. Recover your energy for the next battle.",
        "Even Hunters need time to recover. Enjoy your well-earned rest.",
        "Recovery is part of growth. Your next battle will come soon.",
        "Take today to recharge. Tomorrow, the adventure continues.",
        "Rest without guilt. Consistency includes recovery.",
        "Put down your weapon, brave Hunter. Today, we rest in the safety of the guild.",
        "A good sleep and restful contemplation build the strongest heroes."
    )

    // 3. Before Study Reminder
    private val beforeStudyReminders = listOf(
        "Prepare yourself. Your battle begins soon.",
        "Gather your focus. Your Boss is waiting.",
        "Another opportunity to grow stronger is approaching.",
        "Light the candles and clear your desk. The hour of combat is nigh.",
        "Take a deep breath and settle your mind. A session begins shortly.",
        "Prepare your focus arsenal. The study chamber is prepared."
    )

    // 4. Missed Study Reminder / Twilight Alerts
    private val missedStudyReminders = listOf(
        "Your Boss still stands.",
        "The battlefield remains untouched.",
        "Every delay makes tomorrow's challenge harder.",
        "One session today can still change everything.",
        "Do not let the day end in retreat. Slay the procrastination beast!",
        "The study chamber remains cold. Ignite your focus and step inside.",
        "Night falls, but the hero has yet to strike. There is still time!"
    )

    // 5. Red Dungeon Notifications
    private val redDungeonMessages = listOf(
        "The Red Dungeon is growing.",
        "Your unfinished battles are calling.",
        "The Red Dungeon has expanded. Reclaim your progress.",
        "Darkness grows stronger when ignored.",
        "Reduce the Red Dungeon before it becomes overwhelming.",
        "Procrastination corruption has leaked into the valley. Cleanse the Red Dungeon!"
    )

    // 6. Active Boss Progress Motivation
    private val bossProgressMessages = listOf(
        "Victory is within reach.",
        "Only a little remains before this Boss falls.",
        "Finish what you started.",
        "Your relentless study strikes are wearing down the enemy!"
    )

    // 7. Long Streak Motivation
    private val streakMotivationMessages = listOf(
        "Consistency is becoming your greatest strength.",
        "Protect your streak. Every day matters.",
        "You are unstoppable! Keep the flames of focus blazing!"
    )

    // 8. Weekly Goal Notifications
    private val weeklyGoalMessages = listOf(
        "You're almost there. Finish the week strong.",
        "The guild registers magnificent momentum toward this week's objectives!"
    )

    /**
     * Context-Aware Companion Notification Builder
     */
    fun generateNotification(
        profile: UserProfileEntity,
        bosses: List<BossEntity>,
        studiedMinutesToday: Int,
        studiedMinutesThisWeek: Int,
        weeklyTargetMinutes: Int,
        triggerAction: String
    ): Pair<String, String> {

        // Determine if today is a study day or rest day
        val calendar = Calendar.getInstance()
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayAbbreviation = when (todayDayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "Mon"
        }

        val studyDaysList = profile.scheduleDays.split(",").map { it.trim().lowercase() }
        val isStudyDay = studyDaysList.contains(dayAbbreviation.lowercase())
        val dailyTargetMinutes = profile.getTargetMinutesForCalendarDay(todayDayOfWeek).coerceAtLeast(1)

        // Rule: Rest days must never trigger study reminders or aggressive alerts.
        if (!isStudyDay) {
            val title = listOf(
                "🌙 THE HEARTHFIRE'S WARMTH",
                "🏡 COZY TAVERN REST",
                "☕ HUNTER'S RESPITE",
                "🍃 CALM IN THE FOREST"
            ).random(random)
            val message = restDayMessages.random(random)
            return Pair(title, message)
        }

        // --- STUDY DAY DECISIONS ---
        val remainingMinutes = (dailyTargetMinutes - studiedMinutesToday).coerceAtLeast(0)
        val hasCompletedToday = remainingMinutes == 0

        // Determine if there is an active (uncompleted) boss, and find one with some progress if possible
        val activeBosses = bosses.filter { !it.isCompleted }
        val partiallyCompletedBoss = activeBosses.firstOrNull { it.timeSpentSeconds > 0 }

        // Determine weekly progress
        val weeklyProgressPercent = if (weeklyTargetMinutes > 0) {
            (studiedMinutesThisWeek.toFloat() / weeklyTargetMinutes * 100).toInt()
        } else {
            0
        }

        // 1. Check Red Dungeon Crisis first (High priority if uncompleted today)
        if (profile.redDungeonDays > 0 && !hasCompletedToday) {
            val title = listOf(
                "⚠️ RED DUNGEON CORRUPTION",
                "💀 CRISIS IN THE DUNGEON",
                "🔥 INACTIVITY PRESSURE"
            ).random(random)
            
            val randomChoice = random.nextInt(3)
            val message = when (randomChoice) {
                0 -> "The Red Dungeon has expanded to level ${profile.redDungeonDays}. Reclaim your focus before it grows stronger!"
                1 -> "Your unfinished battles are calling. Slay the procrastination beast today to cleanse the red zones!"
                else -> redDungeonMessages.random(random)
            }
            return Pair(title, message)
        }

        // 2. Handle specific trigger actions
        when (triggerAction) {
            NotificationReceiver.ACTION_MORNING_QUEST -> {
                val title = listOf(
                    "⚔️ MORNING QUEST DECREE",
                    "☀️ DAYBREAK FOCUS CAMPAIGN",
                    "🛡️ ADVENTURE CALLS"
                ).random(random)

                val message = if (hasCompletedToday) {
                    "Amazing job! You've already conquered your daily $dailyTargetMinutes-minute study objective today."
                } else {
                    val randomChoice = random.nextInt(3)
                    when (randomChoice) {
                        0 -> "You have a $dailyTargetMinutes-minute battle waiting today. Draw your quill!"
                        1 -> "Today's mission: Study for $dailyTargetMinutes minutes and strengthen your focus attributes."
                        else -> studyDayBriefings.random(random)
                    }
                }
                return Pair(title, message)
            }

            NotificationReceiver.ACTION_BEFORE_STUDY -> {
                // Pre-study ritual reminder
                val title = listOf(
                    "🔔 THE GATHERING HORN",
                    "🕯️ FOCUS RITUAL BEGINS",
                    "⚔️ PREPARATION HOUR"
                ).random(random)

                val message = if (hasCompletedToday) {
                    "Today's victory is secured. You may relax or engage in a free study raid!"
                } else {
                    val randomChoice = random.nextInt(2)
                    if (randomChoice == 0 && partiallyCompletedBoss != null) {
                        "Your duel with ${partiallyCompletedBoss.name} is approaching. Prepare your focus!"
                    } else {
                        beforeStudyReminders.random(random)
                    }
                }
                return Pair(title, message)
            }

            NotificationReceiver.ACTION_EVENING_CAMPAIGN -> {
                // If study target is already completed, celebrate!
                if (hasCompletedToday) {
                    // Check if streak is notable
                    if (profile.currentStreak >= 3) {
                        val title = "🔥 UNSHAKABLE LEGEND"
                        val message = "Your streak has reached ${profile.currentStreak} days! Consistency is becoming your greatest strength."
                        return Pair(title, message)
                    }
                    // Else celebrate weekly goal
                    if (weeklyProgressPercent >= 50) {
                        val title = "📜 GUILD WEEKLY PROGRESS"
                        val message = "You've completed $weeklyProgressPercent% of this week's study goal. Finish the week strong!"
                        return Pair(title, message)
                    }
                    val title = "🏆 DAILY QUEST COMPLETE!"
                    val message = "You have fully defeated today's study targets. Rest well, hero, or farm more gold in the arena."
                    return Pair(title, message)
                }

                // If study is missed/pending in the evening
                val title = listOf(
                    "⏳ TWILIGHT WARNING",
                    "⚔️ THE FIELD OF COMBAT CALLS",
                    "🛡️ LATE HOUR ENGAGEMENT"
                ).random(random)

                val message = if (partiallyCompletedBoss != null) {
                    val progressPercent = (partiallyCompletedBoss.timeSpentSeconds.toFloat() / (partiallyCompletedBoss.requiredMinutes * 60) * 100).toInt().coerceIn(1, 99)
                    val randomChoice = random.nextInt(3)
                    when (randomChoice) {
                        0 -> "You have already defeated $progressPercent% of ${partiallyCompletedBoss.name}. Finish what you started!"
                        1 -> "Only a little remains before ${partiallyCompletedBoss.name} falls. Slay it tonight!"
                        else -> bossProgressMessages.random(random)
                    }
                } else {
                    val randomChoice = random.nextInt(3)
                    when (randomChoice) {
                        0 -> "Only $remainingMinutes minutes of focus remain to salvage today's daily quest. Step into the arena!"
                        1 -> "Your Boss still stands. Procrastination is winning this hour—retaliate!"
                        else -> missedStudyReminders.random(random)
                    }
                }
                return Pair(title, message)
            }
        }

        // Fallback
        return Pair("⚔️ SOLO STUDYING RPG", "Your next focus encounter is waiting. Claim victory!")
    }

    /**
     * Helper to pick a random item from a list
     */
    private fun <T> List<T>.random(random: Random): T {
        return this[random.nextInt(this.size)]
    }
}
