package org.omarchy.theme.sync.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.omarchy.theme.sync.OmarchyThemeSyncService
import java.awt.Component

class ThemeSyncStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Omarchy Theme Sync"

    override fun isAvailable(project: Project): Boolean = true

    override fun isEnabledByDefault(): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = ThemeSyncStatusBarWidget()

    companion object {
        const val WIDGET_ID = "org.omarchy.theme-sync.status"
    }
}

private class ThemeSyncStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var unsubscribe: (() -> Unit)? = null

    override fun ID(): String = ThemeSyncStatusBarWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = "Omarchy: ${OmarchyThemeSyncService.getInstance().currentStatus().kind.label}"

    override fun getTooltipText(): String = OmarchyThemeSyncService.getInstance().currentStatus().message

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun install(statusBar: StatusBar) {
        unsubscribe = OmarchyThemeSyncService.getInstance().subscribeStatus {
            statusBar.updateWidget(ID())
        }
    }

    override fun dispose() {
        unsubscribe?.invoke()
        unsubscribe = null
    }
}
