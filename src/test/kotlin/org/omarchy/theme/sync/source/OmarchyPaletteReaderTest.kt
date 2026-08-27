package org.omarchy.theme.sync.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.omarchy.theme.sync.model.PaletteDiagnostic
import org.omarchy.theme.sync.model.PaletteMode
import org.omarchy.theme.sync.model.PaletteReadResult
import org.omarchy.theme.sync.model.PaletteSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

class OmarchyPaletteReaderTest {
    private val reader = OmarchyPaletteReader()

    @TempDir
    lateinit var directory: Path

    @Test
    fun `reads a complete dark Omarchy palette`() {
        val result = readFixture("dark.toml")

        val success = assertInstanceOf(PaletteReadResult.Success::class.java, result)
        assertEquals(PaletteMode.DARK, success.palette.mode)
        assertEquals("#2c2525", success.palette["background"].hex)
        assertEquals("#bebffd", success.palette["bright_magenta"].hex)
    }

    @Test
    fun `reads a complete light Omarchy palette`() {
        val result = readFixture("light.toml")

        val success = assertInstanceOf(PaletteReadResult.Success::class.java, result)
        assertEquals(PaletteMode.LIGHT, success.palette.mode)
        assertEquals("#fffaf0", success.palette["background"].hex)
    }

    @Test
    fun `rejects invalid color values without producing a palette`() {
        val contents = fixture("dark.toml").replace("accent = \"#f38d70\"", "accent = \"purple\"")
        val result = read(contents)

        val failure = assertInstanceOf(PaletteReadResult.Failure::class.java, result)
        assertEquals(PaletteDiagnostic.Code.INVALID_COLOR, failure.diagnostic.code)
    }

    @Test
    fun `rejects missing mode and missing colors`() {
        val withoutMode = fixture("dark.toml").replaceFirst("mode = \"dark\"\n", "")
        val modeFailure = assertInstanceOf(PaletteReadResult.Failure::class.java, read(withoutMode))
        assertEquals(PaletteDiagnostic.Code.UNSUPPORTED_MODE, modeFailure.diagnostic.code)

        val withoutColor = fixture("dark.toml").replaceFirst("red = \"#fd6883\"\n", "")
        val colorFailure = assertInstanceOf(PaletteReadResult.Failure::class.java, read(withoutColor))
        assertEquals(PaletteDiagnostic.Code.MISSING_COLOR, colorFailure.diagnostic.code)
    }

    @Test
    fun `rejects malformed, nonregular, unreadable, and unsupported sources`() {
        val malformed = assertInstanceOf(PaletteReadResult.Failure::class.java, read("mode = ["))
        assertEquals(PaletteDiagnostic.Code.MALFORMED_TOML, malformed.diagnostic.code)

        val directorySource = directory.resolve("palette-directory")
        Files.createDirectory(directorySource)
        val nonregular = assertInstanceOf(PaletteReadResult.Failure::class.java, reader.read(PaletteSource(directorySource)))
        assertEquals(PaletteDiagnostic.Code.NOT_A_REGULAR_FILE, nonregular.diagnostic.code)

        val unsupported = assertInstanceOf(PaletteReadResult.Failure::class.java, read(fixture("dark.toml").replace("dark", "solarized")))
        assertEquals(PaletteDiagnostic.Code.UNSUPPORTED_MODE, unsupported.diagnostic.code)

        val unreadablePath = directory.resolve("unreadable.toml")
        Files.writeString(unreadablePath, fixture("dark.toml"))
        Files.setPosixFilePermissions(unreadablePath, emptySet<PosixFilePermission>())
        try {
            val unreadable = assertInstanceOf(PaletteReadResult.Failure::class.java, reader.read(PaletteSource(unreadablePath)))
            assertEquals(PaletteDiagnostic.Code.UNREADABLE, unreadable.diagnostic.code)
        } finally {
            Files.setPosixFilePermissions(unreadablePath, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
        }
    }

    @Test
    fun `reads a configured alternate palette path`() {
        val alternatePath = directory.resolve("alternate/colors.toml")
        Files.createDirectories(alternatePath.parent)
        Files.writeString(alternatePath, fixture("light.toml"))

        val result = reader.read(PaletteSource(alternatePath))

        val success = assertInstanceOf(PaletteReadResult.Success::class.java, result)
        assertEquals(alternatePath, success.source.path)
        assertEquals(PaletteMode.LIGHT, success.palette.mode)
    }

    @Test
    fun `normalizes palette identity independent of TOML key order`() {
        val original = assertInstanceOf(PaletteReadResult.Success::class.java, readFixture("dark.toml")).palette
        val reversed = fixture("dark.toml").lineSequence().filter(String::isNotBlank).toList().reversed().joinToString("\n", postfix = "\n")
        val reordered = assertInstanceOf(PaletteReadResult.Success::class.java, read(reversed)).palette
        val changed = assertInstanceOf(PaletteReadResult.Success::class.java, read(fixture("dark.toml").replace("#f38d70", "#ee8d70"))).palette

        assertEquals(original.fingerprint(), reordered.fingerprint())
        assertNotEquals(original.fingerprint(), changed.fingerprint())
    }

    @Test
    fun `reports a missing source as unavailable`() {
        val result = reader.read(PaletteSource(directory.resolve("not-there.toml")))

        val failure = assertInstanceOf(PaletteReadResult.Failure::class.java, result)
        assertEquals(PaletteDiagnostic.Code.UNAVAILABLE, failure.diagnostic.code)
    }

    private fun readFixture(name: String): PaletteReadResult = read(fixture(name))

    private fun read(contents: String): PaletteReadResult {
        val path = directory.resolve("colors.toml")
        Files.writeString(path, contents)
        return reader.read(PaletteSource(path))
    }

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/palettes/$name"))
        .readText()
}
