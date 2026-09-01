package com.amkumirab.solostudying.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    @Test
    fun `primary text colors meet normal text contrast guidance`() {
        assertContrast("primary text on background", TextWhite, BlackFantasyBackground)
        assertContrast("muted text on surface", TextMuted, DarkFantasySurface)
        assertContrast("subtle text on surface", TextSubtle, DarkFantasySurface)
    }

    @Test
    fun `semantic action colors meet normal text contrast guidance`() {
        assertContrast("info action", NeonBlueAccent, InfoContainer)
        assertContrast("success action", RpgEmerald, SuccessContainer)
        assertContrast("warning action", RpgGold, WarningContainer)
        assertContrast("danger action", RpgRuby, DangerContainer)
    }

    @Test
    fun `accent buttons use a readable foreground`() {
        assertContrast("cyan accent button", OnAccent, NeonBlueAccent)
        assertContrast("gold accent button", OnAccent, RpgGold)
        assertContrast("ruby accent button", OnAccent, RpgRuby)
        assertContrast("emerald accent button", OnAccent, RpgEmerald)
    }

    private fun assertContrast(name: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name contrast was $ratio, expected at least 4.5", ratio >= 4.5)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val brighter = maxOf(relativeLuminance(first), relativeLuminance(second))
        val darker = minOf(relativeLuminance(first), relativeLuminance(second))
        return (brighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(component: Float): Double {
            val value = component.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }

        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
}
