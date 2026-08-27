package org.omarchy.theme.sync

import com.intellij.ide.ui.LafManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import org.omarchy.theme.sync.model.PaletteDiagnostic
import org.omarchy.theme.sync.model.PaletteReadResult
import org.omarchy.theme.sync.model.PaletteSource
import org.omarchy.theme.sync.settings.AppearanceSnapshot
import org.omarchy.theme.sync.settings.SynchronizationMode
import org.omarchy.theme.sync.settings.ThemeSyncSettings
import org.omarchy.theme.sync.source.OmarchyPaletteReader
import org.omarchy.theme.sync.source.PaletteMonitor
import org.omarchy.theme.sync.theme.AppliedAppearance
import org.omarchy.theme.sync.theme.AppearanceController
import org.omarchy.theme.sync.theme.IntelliJAppearanceController
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class SynchronizationStatus(val label: String) {
    PAUSED("Paused"),
    WAITING_FOR_PALETTE("Waiting for palette"),
    UNAVAILABLE("Palette unavailable"),
    SYNCHRONIZED("Synchronized"),
    APPLY_FAILED("Apply failed"),
}

data class ThemeSyncStatus(
    val kind: SynchronizationStatus,
    val message: String,
)

@Service(Service.Level.APP)
class OmarchyThemeSyncService : Disposable {
    private val log = Logger.getInstance(OmarchyThemeSyncService::class.java)
    private val settings = service<ThemeSyncSettings>()
    private val reader = OmarchyPaletteReader()
    private val appearanceController: AppearanceController = IntelliJAppearanceController()
    private val statusListeners = CopyOnWriteArraySet<() -> Unit>()
    private val editorSchemeObserver = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "omarchy-theme-editor-scheme-observer").apply { isDaemon = true }
    }
    private val applyingAppearance = AtomicBoolean(false)

    @Volatile
    private var status = ThemeSyncStatus(SynchronizationStatus.PAUSED, "Synchronization is paused.")

    @Volatile
    private var monitor: PaletteMonitor? = null

    @Volatile
    private var appliedAppearance: AppliedAppearance? = null

    private val started = AtomicBoolean(false)

    /** Invoked from the application lifecycle listener after the IDE frame is ready. */
    fun start() = runOnEdt {
        if (!started.compareAndSet(false, true)) return@runOnEdt
        startAppearanceObserver()
        if (settings.state.mode == SynchronizationMode.ENABLED) enableInternal(captureSnapshot = false)
    }

    fun currentStatus(): ThemeSyncStatus = status

    fun subscribeStatus(listener: () -> Unit): () -> Unit {
        statusListeners += listener
        return { statusListeners -= listener }
    }

    fun enable() = runOnEdt { enableInternal(captureSnapshot = true) }

    fun pause() = runOnEdt {
        stopMonitoring()
        settings.state.mode = SynchronizationMode.PAUSED
        settings.state.lastPaletteFingerprint = ""
        appliedAppearance = null
        updateStatus(SynchronizationStatus.PAUSED, "Synchronization is paused; the current Omarchy appearance remains active.")
    }

    fun disableAndRestore() = runOnEdt {
        stopMonitoring()
        settings.state.mode = SynchronizationMode.PAUSED
        val outcome = appearanceController.restore(settings.state.appearanceSnapshot)
        settings.state.appearanceSnapshot = AppearanceSnapshot()
        settings.state.lastPaletteFingerprint = ""
        appliedAppearance = null
        updateStatus(
            SynchronizationStatus.PAUSED,
            outcome.message ?: "Synchronization is disabled and the previous appearance was restored.",
        )
    }

    fun restorePreviousAppearance() = disableAndRestore()

    fun applyCurrentThemeOnce() {
        ApplicationManager.getApplication().executeOnPooledThread {
            handlePaletteRead(reader.read(settings.source()), automatic = false)
        }
    }

    fun updateConfiguration(enabled: Boolean, configuredPath: String) = runOnEdt {
        val normalizedPath = configuredPath.trim()
        val sourceChanged = settings.state.palettePath != normalizedPath
        settings.state.palettePath = normalizedPath
        if (sourceChanged) settings.state.lastPaletteFingerprint = ""
        if (enabled) enableInternal(captureSnapshot = true, restartMonitor = sourceChanged) else disableAndRestore()
    }

    private fun enableInternal(captureSnapshot: Boolean, restartMonitor: Boolean = false) {
        if (captureSnapshot && settings.state.appearanceSnapshot.isEmpty()) {
            settings.state.appearanceSnapshot = appearanceController.snapshot()
        }
        settings.state.mode = SynchronizationMode.ENABLED
        updateStatus(SynchronizationStatus.WAITING_FOR_PALETTE, "Reading the Omarchy palette…")
        if (restartMonitor || monitor == null) {
            stopMonitoring()
            monitor = PaletteMonitor(
                sourceProvider = settings::source,
                reader = reader,
                onResult = { result -> handlePaletteRead(result, automatic = true) },
            ).also(PaletteMonitor::start)
        } else {
            monitor?.refresh()
        }
    }

    private fun handlePaletteRead(result: PaletteReadResult, automatic: Boolean) {
        when (result) {
            is PaletteReadResult.Success -> {
                if (automatic && settings.state.mode != SynchronizationMode.ENABLED) return
                val fingerprint = result.palette.fingerprint()
                if (automatic && fingerprint == settings.state.lastPaletteFingerprint && appliedAppearance != null) {
                    updateStatus(SynchronizationStatus.SYNCHRONIZED, "Omarchy palette is already applied.")
                    return
                }
                runOnEdt {
                    applyPalette(result, fingerprint, automatic)
                }
            }

            is PaletteReadResult.Failure -> {
                if (automatic && settings.state.mode != SynchronizationMode.ENABLED) return
                val kind = if (result.diagnostic.code == PaletteDiagnostic.Code.UNAVAILABLE) {
                    SynchronizationStatus.UNAVAILABLE
                } else {
                    SynchronizationStatus.WAITING_FOR_PALETTE
                }
                updateStatus(kind, result.diagnostic.message, notify = true)
            }
        }
    }

    private fun applyPalette(result: PaletteReadResult.Success, fingerprint: String, automatic: Boolean) {
        if (automatic && settings.state.mode != SynchronizationMode.ENABLED) return
        if (settings.state.appearanceSnapshot.isEmpty()) settings.state.appearanceSnapshot = appearanceController.snapshot()
        applyingAppearance.set(true)
        try {
            appliedAppearance = appearanceController.apply(result.palette)
            settings.state.lastPaletteFingerprint = fingerprint
            updateStatus(SynchronizationStatus.SYNCHRONIZED, "Synchronized with ${result.source.path.fileName}.")
        } catch (exception: Exception) {
            log.warn("Could not apply Omarchy palette", exception)
            updateStatus(SynchronizationStatus.APPLY_FAILED, "Could not apply the Omarchy palette: ${exception.message}", notify = true)
        } finally {
            applyingAppearance.set(false)
        }
    }

    /** Polling avoids the legacy LafManager listener API, and also catches editor-only changes. */
    private fun startAppearanceObserver() {
        editorSchemeObserver.scheduleWithFixedDelay({
            val applied = appliedAppearance ?: return@scheduleWithFixedDelay
            val actualTheme = LafManager.getInstance().currentUIThemeLookAndFeel?.id
            val actualScheme = EditorColorsManager.getInstance().globalScheme.name
            if (
                shouldTreatAsManualChange(applied.uiThemeId, actualTheme) ||
                shouldTreatAsManualChange(applied.editorSchemeName, actualScheme)
            ) {
                runOnEdt(::pauseForManualAppearanceChange)
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun shouldTreatAsManualChange(expected: String?, actual: String?): Boolean =
        settings.state.mode == SynchronizationMode.ENABLED &&
            !applyingAppearance.get() &&
            expected != null &&
            expected != actual

    private fun pauseForManualAppearanceChange() {
        if (settings.state.mode != SynchronizationMode.ENABLED) return
        stopMonitoring()
        settings.state.mode = SynchronizationMode.PAUSED
        settings.state.lastPaletteFingerprint = ""
        settings.state.appearanceSnapshot = AppearanceSnapshot()
        appliedAppearance = null
        updateStatus(SynchronizationStatus.PAUSED, "Synchronization paused because you selected a different IDE appearance.", notify = true)
    }

    private fun stopMonitoring() {
        monitor?.close()
        monitor = null
    }

    private fun updateStatus(kind: SynchronizationStatus, message: String, notify: Boolean = false) {
        val changed = status != ThemeSyncStatus(kind, message)
        status = ThemeSyncStatus(kind, message)
        if (!changed) return
        runOnEdt {
            statusListeners.forEach { it.invoke() }
            if (notify) {
                val type = if (kind == SynchronizationStatus.APPLY_FAILED || kind == SynchronizationStatus.UNAVAILABLE) NotificationType.WARNING else NotificationType.INFORMATION
                NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
                    .createNotification(message, type)
                    .notify(null)
            }
        }
    }

    private fun runOnEdt(action: () -> Unit) {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) action() else application.invokeLater(action)
    }

    override fun dispose() {
        stopMonitoring()
        editorSchemeObserver.shutdownNow()
    }

    companion object {
        const val NOTIFICATION_GROUP = "Omarchy Theme Sync"

        fun getInstance(): OmarchyThemeSyncService = service()
    }
}

private fun AppearanceSnapshot.isEmpty(): Boolean = uiThemeId == null && editorSchemeName == null
