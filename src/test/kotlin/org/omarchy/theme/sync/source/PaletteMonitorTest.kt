package org.omarchy.theme.sync.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.omarchy.theme.sync.model.PaletteReadResult
import org.omarchy.theme.sync.model.PaletteSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PaletteMonitorTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `discovers a replacement theme directory once a complete palette is available`() {
        val current = directory.resolve("current")
        val theme = current.resolve("theme")
        Files.createDirectories(theme)
        Files.writeString(theme.resolve("colors.toml"), fixture("dark.toml"))
        val source = PaletteSource(theme.resolve("colors.toml"))
        val successes = CopyOnWriteArrayList<String>()
        val changed = CountDownLatch(1)
        val monitor = PaletteMonitor(
            sourceProvider = { source },
            reader = OmarchyPaletteReader(),
            onResult = { result ->
                if (result is PaletteReadResult.Success) {
                    successes += result.palette.fingerprint()
                    if (successes.distinct().size == 2) changed.countDown()
                }
            },
            debounceMillis = 25,
            maxRetries = 8,
        )

        try {
            monitor.start()
            waitFor { successes.isNotEmpty() }
            val nextTheme = current.resolve("next-theme")
            Files.createDirectories(nextTheme)
            Files.writeString(nextTheme.resolve("colors.toml"), fixture("light.toml"))
            Files.delete(theme.resolve("colors.toml"))
            Files.delete(theme)
            Files.move(nextTheme, theme)

            assertTrue(changed.await(5, TimeUnit.SECONDS), "The monitor did not discover the replacement palette")
            Thread.sleep(150)
            assertEquals(2, successes.size)
        } finally {
            monitor.close()
        }
    }

    @Test
    fun `does not notify when watched files leave the palette unchanged`() {
        val theme = directory.resolve("current/theme")
        Files.createDirectories(theme)
        val paletteFile = theme.resolve("colors.toml")
        val contents = fixture("dark.toml")
        Files.writeString(paletteFile, contents)
        val successes = CopyOnWriteArrayList<String>()
        val monitor = PaletteMonitor(
            sourceProvider = { PaletteSource(paletteFile) },
            reader = OmarchyPaletteReader(),
            onResult = { result -> if (result is PaletteReadResult.Success) successes += result.palette.fingerprint() },
            debounceMillis = 25,
        )

        try {
            monitor.start()
            waitFor { successes.size == 1 }
            Files.writeString(theme.resolve("unrelated-state"), "changed")
            Files.writeString(paletteFile, contents)
            Thread.sleep(200)

            assertEquals(1, successes.size)
        } finally {
            monitor.close()
        }
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(condition(), "The monitor did not read the initial palette")
    }

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/palettes/$name")).readText()
}
