package org.omarchy.theme.sync.theme

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.IconPathPatcher
import org.omarchy.theme.sync.model.OmarchyPalette
import org.omarchy.theme.sync.model.PaletteMode
import org.omarchy.theme.sync.settings.AppearanceSnapshot
import java.io.ByteArrayInputStream
import javax.swing.UIManager

data class AppliedAppearance(
    val uiThemeId: String?,
    val editorSchemeName: String,
)

data class RestoreOutcome(
    val restoredUiTheme: Boolean,
    val restoredEditorScheme: Boolean,
    val message: String? = null,
)

interface AppearanceController {
    fun snapshot(): AppearanceSnapshot
    fun apply(palette: OmarchyPalette): AppliedAppearance
    fun restore(snapshot: AppearanceSnapshot): RestoreOutcome
}

class IntelliJAppearanceController(
    private val runtimeThemeLoader: RuntimeThemeLoader = RuntimeThemeLoader(),
) : AppearanceController {
    override fun snapshot(): AppearanceSnapshot {
        assertEdt()
        val lafManager = LafManager.getInstance()
        return AppearanceSnapshot(
            uiThemeId = lafManager.currentUIThemeLookAndFeel?.id
                ?: "$CLASSIC_LAF_PREFIX${UIManager.getLookAndFeel().javaClass.name}",
            editorSchemeName = EditorColorsManager.getInstance().globalScheme.name,
        )
    }

    override fun apply(palette: OmarchyPalette): AppliedAppearance {
        assertEdt()
        val lafManager = LafManager.getInstance()
        val themeInfo = runtimeThemeLoader.load(OmarchyThemeMapper.uiTheme(palette))
        lafManager.setCurrentUIThemeLookAndFeel(themeInfo)

        val editorColorsManager = EditorColorsManager.getInstance()
        val baseScheme = matchingBaseScheme(editorColorsManager, palette.mode)
        val scheme = OmarchyThemeMapper.editorScheme(palette, baseScheme)
        editorColorsManager.addColorScheme(scheme)
        editorColorsManager.setGlobalScheme(scheme)
        lafManager.updateUI()
        lafManager.repaintUI()
        return AppliedAppearance(themeInfo.id, scheme.name)
    }

    override fun restore(snapshot: AppearanceSnapshot): RestoreOutcome {
        assertEdt()
        val lafManager = LafManager.getInstance()
        val editorColorsManager = EditorColorsManager.getInstance()
        var uiRestored = false
        var schemeRestored = false
        val issues = mutableListOf<String>()

        snapshot.uiThemeId?.let { id ->
            if (id.startsWith(CLASSIC_LAF_PREFIX)) {
                val className = id.removePrefix(CLASSIC_LAF_PREFIX)
                try {
                    UIManager.setLookAndFeel(className)
                    uiRestored = true
                } catch (_: Exception) {
                    issues += "The previous IDE look and feel is no longer available."
                }
            } else {
                val theme = lafManager.installedThemes.firstOrNull { it.id == id }
                if (theme == null) issues += "The previous IDE theme is no longer available." else {
                    lafManager.setCurrentUIThemeLookAndFeel(theme)
                    uiRestored = true
                }
            }
        }
        snapshot.editorSchemeName?.let { name ->
            val scheme = editorColorsManager.getScheme(name)
            if (scheme == null) issues += "The previous editor scheme is no longer available." else {
                editorColorsManager.setGlobalScheme(scheme)
                schemeRestored = true
            }
        }
        lafManager.updateUI()
        lafManager.repaintUI()
        return RestoreOutcome(uiRestored, schemeRestored, issues.takeIf { it.isNotEmpty() }?.joinToString(" "))
    }

    private fun matchingBaseScheme(manager: EditorColorsManager, mode: PaletteMode): EditorColorsScheme {
        val expectedName = if (mode == PaletteMode.DARK) "Darcula" else "Default"
        return manager.getScheme(expectedName) ?: manager.globalScheme
    }

    private fun assertEdt() {
        check(ApplicationManager.getApplication().isDispatchThread) { "Appearance changes must run on the EDT" }
    }

    private companion object {
        const val CLASSIC_LAF_PREFIX = "class:"
    }
}

/**
 * Runtime theme loading is kept behind this adapter because the public theme
 * provider extension is resource-backed and cannot follow an external palette.
 */
class RuntimeThemeLoader {
    private val noOpIconPathPatcher = object : IconPathPatcher() {
        override fun patchPath(path: String, classLoader: ClassLoader?): String? = null
    }

    fun load(document: RuntimeThemeDocument): UIThemeLookAndFeelInfo {
        val loaderClass = Class.forName("com.intellij.ide.ui.laf.TempUIThemeLookAndFeelInfo")
        val method = loaderClass.methods.singleOrNull { candidate ->
            candidate.name == "loadTempTheme" && candidate.parameterCount == 2
        } ?: error("The IntelliJ runtime theme loader is unavailable")
        val input = ByteArrayInputStream(document.json.toByteArray(Charsets.UTF_8))
        val uiTheme = method.invoke(null, input, noOpIconPathPatcher)
        val infoClass = Class.forName("com.intellij.ide.ui.laf.UIThemeLookAndFeelInfoImpl")
        val constructor = infoClass.constructors.singleOrNull { candidate ->
            candidate.parameterCount == 1 && candidate.parameterTypes[0].isAssignableFrom(uiTheme.javaClass)
        } ?: error("The IntelliJ runtime theme look-and-feel adapter is unavailable")
        return constructor.newInstance(uiTheme) as UIThemeLookAndFeelInfo
    }
}
