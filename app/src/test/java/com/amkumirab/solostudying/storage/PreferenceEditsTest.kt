package com.amkumirab.solostudying.storage

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class PreferenceEditsTest {
    @Test fun `checked edit commits one complete batch without asynchronous apply`() {
        val fixture = RecordingPreferences(true)
        assertTrue(fixture.preferences.commitChanges {
            putBoolean("enabled", true)
            putLong("finished", 60L)
        })
        assertEquals(mapOf("enabled" to true, "finished" to 60L), fixture.committed)
        assertEquals(1, fixture.commits)
        assertEquals(0, fixture.applies)
    }

    @Test fun `checked edit returns native commit failure without an apply fallback`() {
        val fixture = RecordingPreferences(false)
        assertFalse(fixture.preferences.commitChanges { putBoolean("enabled", true) })
        assertEquals(1, fixture.commits)
        assertEquals(0, fixture.applies)
    }

    @Test fun `failed editing never commits a partial batch`() {
        val fixture = RecordingPreferences(true)
        assertThrows(IllegalArgumentException::class.java) {
            fixture.preferences.commitChanges {
                putBoolean("enabled", true)
                throw IllegalArgumentException("Invalid fixture input")
            }
        }
        assertEquals(0, fixture.commits)
        assertEquals(0, fixture.applies)
    }

    private class RecordingPreferences(private val success: Boolean) {
        var commits = 0
        var applies = 0
        val pending = mutableMapOf<String, Any?>()
        val committed = mutableMapOf<String, Any?>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, arguments ->
            when (method.name) {
                "putBoolean", "putLong" -> {
                    pending[arguments!![0] as String] = arguments[1]
                    proxy
                }
                "commit" -> { commits++; if (success) committed.putAll(pending); success }
                "apply" -> { applies++; null }
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, _ ->
            check(method.name == "edit") { "Unexpected preference call: ${method.name}" }
            editor
        } as SharedPreferences
    }
}
