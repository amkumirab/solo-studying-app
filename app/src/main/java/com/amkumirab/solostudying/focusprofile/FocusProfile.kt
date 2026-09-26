package com.amkumirab.solostudying.focusprofile

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class FocusProfileType {
    SINGLE,
    CYCLE,
}

data class FocusProfile(
    val id: String,
    val name: String,
    val type: FocusProfileType,
    val focusMinutes: Int,
    val breakMinutes: Int = 5,
    val rounds: Int = 4,
    val skillId: Int? = null,
    val useFocusShield: Boolean = false,
    val useStrictFocus: Boolean = false,
    val createdAtMillis: Long,
    val lastUsedAtMillis: Long = 0L,
) {
    init {
        require(id.isNotBlank())
        require(name.trim().length in 1..MAX_NAME_LENGTH)
        require(focusMinutes in 1..480)
        require(breakMinutes in 1..60)
        require(rounds in 2..12)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

class FocusProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): List<FocusProfile> {
        val raw = preferences.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching { array.getJSONObject(index).toProfile() }
                        .getOrNull()
                        ?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun write(profiles: List<FocusProfile>) {
        val array = JSONArray()
        profiles.forEach { profile -> array.put(profile.toJson()) }
        preferences.edit { putString(KEY_PROFILES, array.toString()) }
    }

    fun clear() = preferences.edit { remove(KEY_PROFILES) }

    private fun FocusProfile.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name.trim())
        put("type", type.name)
        put("focusMinutes", focusMinutes)
        put("breakMinutes", breakMinutes)
        put("rounds", rounds)
        put("skillId", skillId ?: JSONObject.NULL)
        put("useFocusShield", useFocusShield)
        put("useStrictFocus", useStrictFocus)
        put("createdAtMillis", createdAtMillis)
        put("lastUsedAtMillis", lastUsedAtMillis)
    }

    private fun JSONObject.toProfile() = FocusProfile(
        id = getString("id"),
        name = getString("name"),
        type = FocusProfileType.valueOf(getString("type")),
        focusMinutes = getInt("focusMinutes"),
        breakMinutes = optInt("breakMinutes", 5),
        rounds = optInt("rounds", 4),
        skillId = if (isNull("skillId")) null else getInt("skillId"),
        useFocusShield = optBoolean("useFocusShield", false),
        useStrictFocus = optBoolean("useStrictFocus", false),
        createdAtMillis = getLong("createdAtMillis"),
        lastUsedAtMillis = optLong("lastUsedAtMillis", 0L),
    )

    companion object {
        const val PREFERENCES_NAME = "solo_studying_focus_profiles"
        private const val KEY_PROFILES = "profiles"
    }
}
