package org.omarchy.theme.sync.model

import java.awt.Color
import java.nio.file.Path
import java.security.MessageDigest

enum class PaletteMode {
    DARK,
    LIGHT;

    companion object {
        fun parse(value: String?): PaletteMode? = when (value?.lowercase()) {
            "dark" -> DARK
            "light" -> LIGHT
            else -> null
        }
    }
}

@ConsistentCopyVisibility
data class RgbaColor private constructor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    val hex: String = if (alpha == 255) {
        "#%02x%02x%02x".format(red, green, blue)
    } else {
        "#%02x%02x%02x%02x".format(red, green, blue, alpha)
    }

    fun toAwtColor(): Color = Color(red, green, blue, alpha)

    fun relativeLuminance(): Double = listOf(red, green, blue)
        .map { channel ->
            val normalized = channel / 255.0
            if (normalized <= 0.03928) normalized / 12.92 else ((normalized + 0.055) / 1.055).let { it * it * it * it }
        }
        .let { (0.2126 * it[0]) + (0.7152 * it[1]) + (0.0722 * it[2]) }

    fun contrastRatio(other: RgbaColor): Double {
        val lighter = maxOf(relativeLuminance(), other.relativeLuminance())
        val darker = minOf(relativeLuminance(), other.relativeLuminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    companion object {
        private val HEX = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

        fun parse(value: String?): RgbaColor? {
            val match = value?.trim()?.let(HEX::matchEntire) ?: return null
            val digits = match.groupValues[1]
            return RgbaColor(
                red = digits.substring(0, 2).toInt(16),
                green = digits.substring(2, 4).toInt(16),
                blue = digits.substring(4, 6).toInt(16),
                alpha = if (digits.length == 8) digits.substring(6, 8).toInt(16) else 255,
            )
        }
    }
}

data class OmarchyPalette(
    val mode: PaletteMode,
    val colors: Map<String, RgbaColor>,
) {
    init {
        require(REQUIRED_COLOR_KEYS.all(colors::containsKey)) { "Palette is missing a required color" }
    }

    operator fun get(key: String): RgbaColor = requireNotNull(colors[key]) { "Unknown palette color: $key" }

    fun normalizedContent(): String = buildString {
        append("mode=").append(mode.name.lowercase()).append('\n')
        colors.toSortedMap().forEach { (key, color) -> append(key).append('=').append(color.hex).append('\n') }
    }

    fun fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(normalizedContent().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        val REQUIRED_COLOR_KEYS: Set<String> = setOf(
            "background", "foreground", "accent", "selection", "muted",
            "red", "yellow", "orange", "green", "cyan", "blue", "magenta", "brown",
            "bright_red", "bright_yellow", "bright_green", "bright_cyan", "bright_blue", "bright_magenta",
        )
    }
}

data class PaletteSource(val path: Path) {
    companion object {
        fun default(): PaletteSource = PaletteSource(
            Path.of(
                System.getProperty("user.home"),
                ".local", "state", "omarchy", "current", "theme", "colors.toml",
            ),
        )
    }
}

sealed interface PaletteReadResult {
    data class Success(val palette: OmarchyPalette, val source: PaletteSource) : PaletteReadResult

    data class Failure(
        val source: PaletteSource,
        val diagnostic: PaletteDiagnostic,
    ) : PaletteReadResult
}

data class PaletteDiagnostic(
    val code: Code,
    val message: String,
) {
    enum class Code {
        UNAVAILABLE,
        NOT_A_REGULAR_FILE,
        UNREADABLE,
        MALFORMED_TOML,
        UNSUPPORTED_MODE,
        MISSING_COLOR,
        INVALID_COLOR,
    }
}
