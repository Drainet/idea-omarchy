package org.omarchy.theme.sync.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.omarchy.theme.sync.model.OmarchyPalette
import org.omarchy.theme.sync.model.PaletteMode
import org.omarchy.theme.sync.model.RgbaColor

class OmarchyThemeMapperTest {
    @Test
    fun `uses palette mode and semantic colors in the runtime UI document`() {
        val document = OmarchyThemeMapper.uiTheme(palette(PaletteMode.DARK))

        assertTrue(document.json.contains("\"dark\": true"))
        assertTrue(document.json.contains("\"parentTheme\": \"Darcula\""))
        assertTrue(document.json.contains("\"Panel.background\":\"background\""))
        assertTrue(document.json.contains("\"EditorTabs.underlineColor\":\"accent\""))
        assertTrue(document.json.contains("\"Notification.background\":\"selection\""))
    }

    @Test
    fun `uses a contrast safe foreground when a palette foreground is unreadable`() {
        val colors = colors().toMutableMap().apply {
            this["foreground"] = RgbaColor.parse("#111111")!!
            this["background"] = RgbaColor.parse("#101010")!!
        }

        val document = OmarchyThemeMapper.uiTheme(OmarchyPalette(PaletteMode.DARK, colors))

        assertTrue(document.json.contains("\"foreground\":\"#ffffff\""))
    }

    @Test
    fun `uses a dark contrast fallback for an unreadable light palette`() {
        val colors = colors().toMutableMap().apply {
            this["foreground"] = RgbaColor.parse("#fefefe")!!
            this["background"] = RgbaColor.parse("#ffffff")!!
        }

        val document = OmarchyThemeMapper.uiTheme(OmarchyPalette(PaletteMode.LIGHT, colors))

        assertTrue(document.json.contains("\"parentTheme\": \"IntelliJLight\""))
        assertTrue(document.json.contains("\"foreground\":\"#000000\""))
    }

    @Test
    fun `accepts rgba colors and computes contrast`() {
        val translucent = RgbaColor.parse("#11223399")!!
        val white = RgbaColor.parse("#ffffff")!!

        assertEquals("#11223399", translucent.hex)
        assertTrue(white.contrastRatio(RgbaColor.parse("#000000")!!) > 20)
    }

    private fun palette(mode: PaletteMode): OmarchyPalette = OmarchyPalette(mode, colors())

    private fun colors(): Map<String, RgbaColor> = OmarchyPalette.REQUIRED_COLOR_KEYS.associateWith { key ->
        RgbaColor.parse(
            when (key) {
                "background" -> "#202020"
                "foreground" -> "#eeeeee"
                "accent" -> "#55aaff"
                "selection" -> "#333f55"
                "muted" -> "#888888"
                "red", "bright_red" -> "#ff5555"
                "yellow", "bright_yellow" -> "#f1fa8c"
                "orange" -> "#ffb86c"
                "green", "bright_green" -> "#50fa7b"
                "cyan", "bright_cyan" -> "#8be9fd"
                "blue", "bright_blue" -> "#55aaff"
                "magenta", "bright_magenta" -> "#bd93f9"
                "brown" -> "#a07050"
                else -> error("Unexpected required color: $key")
            },
        )!!
    }
}
