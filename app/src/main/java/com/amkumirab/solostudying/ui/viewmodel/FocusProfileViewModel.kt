package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.amkumirab.solostudying.focusprofile.FocusProfile
import com.amkumirab.solostudying.focusprofile.FocusProfileStore
import com.amkumirab.solostudying.focusprofile.FocusProfileType
import java.util.UUID

class FocusProfileViewModel(
    context: Context,
    private val store: FocusProfileStore = FocusProfileStore(context),
    private val clock: () -> Long = System::currentTimeMillis,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    var profiles by mutableStateOf(sorted(store.read()))
        private set

    fun save(
        existingId: String?,
        name: String,
        type: FocusProfileType,
        focusMinutes: Int,
        breakMinutes: Int,
        rounds: Int,
        skillId: Int?,
        useFocusShield: Boolean,
        useStrictFocus: Boolean,
    ) {
        val trimmedName = name.trim()
        require(trimmedName.length in 1..FocusProfile.MAX_NAME_LENGTH)
        val existing = profiles.firstOrNull { it.id == existingId }
        val profile = FocusProfile(
            id = existing?.id ?: idProvider(),
            name = trimmedName,
            type = type,
            focusMinutes = focusMinutes,
            breakMinutes = breakMinutes,
            rounds = rounds,
            skillId = skillId,
            useFocusShield = useFocusShield,
            useStrictFocus = useStrictFocus,
            createdAtMillis = existing?.createdAtMillis ?: clock(),
            lastUsedAtMillis = existing?.lastUsedAtMillis ?: 0L,
        )
        update(profiles.filterNot { it.id == profile.id } + profile)
    }

    fun delete(profile: FocusProfile) {
        update(profiles.filterNot { it.id == profile.id })
    }

    fun markUsed(profile: FocusProfile) {
        val current = profiles.firstOrNull { it.id == profile.id } ?: return
        update(profiles.filterNot { it.id == profile.id } + current.copy(lastUsedAtMillis = clock()))
    }

    private fun update(value: List<FocusProfile>) {
        profiles = sorted(value)
        store.write(profiles)
    }

    companion object {
        private fun sorted(profiles: List<FocusProfile>): List<FocusProfile> = profiles.sortedWith(
            compareByDescending<FocusProfile> { it.lastUsedAtMillis }
                .thenByDescending { it.createdAtMillis },
        )
    }
}
