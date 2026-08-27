package org.omarchy.theme.sync.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import org.omarchy.theme.sync.model.PaletteSource

enum class SynchronizationMode {
    ENABLED,
    PAUSED,
}

data class AppearanceSnapshot(
    var uiThemeId: String? = null,
    var editorSchemeName: String? = null,
)

@State(name = "OmarchyThemeSyncSettings", storages = [Storage("omarchy-theme-sync.xml")])
class ThemeSyncSettings : PersistentStateComponent<ThemeSyncSettings.StoredState> {
    class StoredState {
        var mode: SynchronizationMode = SynchronizationMode.PAUSED
        var palettePath: String = ""
        var lastPaletteFingerprint: String = ""
        var appearanceSnapshot: AppearanceSnapshot = AppearanceSnapshot()
    }

    private var storedState = StoredState()

    override fun getState(): StoredState = storedState

    override fun loadState(state: StoredState) {
        storedState = state
    }

    fun source(): PaletteSource = PaletteSource(
        storedState.palettePath.takeIf(String::isNotBlank)?.let(java.nio.file.Path::of) ?: PaletteSource.default().path,
    )

    companion object {
        fun getInstance(): ThemeSyncSettings = service()
    }
}
