package org.omarchy.theme.sync.source

import org.omarchy.theme.sync.model.OmarchyPalette
import org.omarchy.theme.sync.model.PaletteDiagnostic
import org.omarchy.theme.sync.model.PaletteMode
import org.omarchy.theme.sync.model.PaletteReadResult
import org.omarchy.theme.sync.model.PaletteSource
import org.omarchy.theme.sync.model.RgbaColor
import org.tomlj.Toml
import java.nio.file.Files

class OmarchyPaletteReader {
    fun read(source: PaletteSource): PaletteReadResult {
        val path = source.path.toAbsolutePath().normalize()
        if (!Files.exists(path)) return failure(source, PaletteDiagnostic.Code.UNAVAILABLE, "Palette file is not available: $path")
        if (!Files.isRegularFile(path)) return failure(source, PaletteDiagnostic.Code.NOT_A_REGULAR_FILE, "Palette path is not a regular file: $path")
        if (!Files.isReadable(path)) return failure(source, PaletteDiagnostic.Code.UNREADABLE, "Palette file cannot be read: $path")

        val result = try {
            Files.newBufferedReader(path).use(Toml::parse)
        } catch (exception: Exception) {
            return failure(source, PaletteDiagnostic.Code.UNREADABLE, "Could not read palette: ${exception.message}")
        }
        if (result.hasErrors()) {
            return failure(source, PaletteDiagnostic.Code.MALFORMED_TOML, result.errors().joinToString { it.toString() })
        }

        val mode = PaletteMode.parse(result.getString("mode"))
            ?: return failure(source, PaletteDiagnostic.Code.UNSUPPORTED_MODE, "Palette mode must be 'dark' or 'light'")
        val colors = linkedMapOf<String, RgbaColor>()
        for (key in OmarchyPalette.REQUIRED_COLOR_KEYS) {
            val raw = result.getString(key)
                ?: return failure(source, PaletteDiagnostic.Code.MISSING_COLOR, "Palette is missing '$key'")
            val color = RgbaColor.parse(raw)
                ?: return failure(source, PaletteDiagnostic.Code.INVALID_COLOR, "Palette color '$key' is not a valid RGB or RGBA hex value")
            colors[key] = color
        }
        return PaletteReadResult.Success(OmarchyPalette(mode, colors), source)
    }

    private fun failure(source: PaletteSource, code: PaletteDiagnostic.Code, message: String): PaletteReadResult.Failure =
        PaletteReadResult.Failure(source, PaletteDiagnostic(code, message))
}
