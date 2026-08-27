package org.omarchy.theme.sync.source

import org.omarchy.theme.sync.model.PaletteReadResult
import org.omarchy.theme.sync.model.PaletteSource
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches a stable parent directory rather than only colors.toml. Omarchy replaces
 * the theme directory during a switch, which invalidates a file-only watch.
 */
class PaletteMonitor(
    private val sourceProvider: () -> PaletteSource,
    private val reader: OmarchyPaletteReader,
    private val onResult: (PaletteReadResult) -> Unit,
    private val debounceMillis: Long = 200,
    private val maxRetries: Int = 4,
) : AutoCloseable {
    private val active = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "omarchy-theme-palette-monitor").apply { isDaemon = true }
    }
    private val watchExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "omarchy-theme-palette-watch").apply { isDaemon = true }
    }
    private var watchService: WatchService? = null
    private var watchKey: WatchKey? = null
    private var registeredDirectory: Path? = null
    private var pendingReconcile: ScheduledFuture<*>? = null
    private var lastSuccessfulFingerprint: String? = null

    fun start() {
        if (!active.compareAndSet(false, true)) return
        executor.execute {
            installWatch()
            scheduleReconcile(0)
            watchExecutor.execute(::watchLoop)
        }
    }

    fun refresh() {
        if (active.get()) executor.execute { scheduleReconcile(0) }
    }

    private fun watchLoop() {
        while (active.get()) {
            val key = try {
                watchService?.take()
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                if (active.get()) scheduleReconcile(0)
                null
            } ?: continue
            key.pollEvents()
            if (!key.reset()) {
                executor.execute {
                    installWatch(force = true)
                    scheduleReconcile(0)
                }
            } else {
                executor.execute { scheduleReconcile(debounceMillis) }
            }
        }
    }

    private fun scheduleReconcile(delayMillis: Long, attempt: Int = 0) {
        if (!active.get() || executor.isShutdown) return
        pendingReconcile?.cancel(false)
        pendingReconcile = executor.schedule({ reconcile(attempt) }, delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun reconcile(attempt: Int) {
        if (!active.get()) return
        installWatch()
        val result = reader.read(sourceProvider())
        if (result is PaletteReadResult.Success) {
            val fingerprint = result.palette.fingerprint()
            if (fingerprint != lastSuccessfulFingerprint) {
                lastSuccessfulFingerprint = fingerprint
                onResult(result)
            }
        } else {
            onResult(result)
        }
        if (result is PaletteReadResult.Failure && attempt < maxRetries) {
            scheduleReconcile((attempt + 1) * debounceMillis, attempt + 1)
        }
    }

    private fun installWatch(force: Boolean = false) {
        val directory = nearestExistingDirectory(sourceProvider().path.parent ?: sourceProvider().path) ?: return
        if (!force && directory == registeredDirectory && watchKey?.isValid == true) return
        watchKey?.cancel()
        watchService?.close()
        watchService = FileSystems.getDefault().newWatchService()
        watchKey = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        registeredDirectory = directory
    }

    private fun nearestExistingDirectory(initial: Path): Path? {
        var candidate: Path? = initial.toAbsolutePath().normalize()
        while (candidate != null && !Files.isDirectory(candidate)) candidate = candidate.parent
        return candidate
    }

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        pendingReconcile?.cancel(true)
        watchKey?.cancel()
        watchService?.close()
        executor.shutdownNow()
        watchExecutor.shutdownNow()
    }
}
