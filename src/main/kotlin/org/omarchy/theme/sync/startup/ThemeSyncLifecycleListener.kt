package org.omarchy.theme.sync.startup

import com.intellij.ide.AppLifecycleListener
import org.omarchy.theme.sync.OmarchyThemeSyncService

/** Starts synchronization only once IntelliJ has created its first application frame. */
class ThemeSyncLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: List<String>) {
        OmarchyThemeSyncService.getInstance().start()
    }
}
