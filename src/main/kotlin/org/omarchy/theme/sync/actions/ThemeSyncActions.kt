package org.omarchy.theme.sync.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.omarchy.theme.sync.OmarchyThemeSyncService

class EnableSynchronizationAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) = OmarchyThemeSyncService.getInstance().enable()
}

class PauseSynchronizationAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) = OmarchyThemeSyncService.getInstance().pause()
}

class ApplyCurrentThemeAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) = OmarchyThemeSyncService.getInstance().applyCurrentThemeOnce()
}

class RestorePreviousAppearanceAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) = OmarchyThemeSyncService.getInstance().restorePreviousAppearance()
}
