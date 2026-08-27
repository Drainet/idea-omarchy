package org.omarchy.theme.sync.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import org.omarchy.theme.sync.OmarchyThemeSyncService
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class OmarchyThemeConfigurable : SearchableConfigurable {
    private val settings = ThemeSyncSettings.getInstance()
    private val service = OmarchyThemeSyncService.getInstance()
    private var panel: JPanel? = null
    private var automaticSync: JBCheckBox? = null
    private var sourcePath: JBTextField? = null
    private var statusLabel: JBLabel? = null
    private var unsubscribe: (() -> Unit)? = null

    override fun getId(): String = "org.omarchy.theme-sync.settings"

    override fun getDisplayName(): String = "Omarchy Theme Sync"

    override fun createComponent(): JComponent {
        val syncCheckbox = JBCheckBox("Synchronize automatically with Omarchy")
        val pathField = JBTextField()
        val status = JBLabel()
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JButton("Apply current palette once").apply { addActionListener { service.applyCurrentThemeOnce() } })
            add(JButton("Pause").apply { addActionListener { service.pause() } })
            add(JButton("Restore previous appearance").apply { addActionListener { service.restorePreviousAppearance() } })
        }
        val content = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            add(syncCheckbox)
            add(JBLabel("Palette file (leave empty for Omarchy's default):"))
            add(pathField)
            add(actions)
            add(status)
        }
        automaticSync = syncCheckbox
        sourcePath = pathField
        statusLabel = status
        panel = JPanel(BorderLayout()).apply { add(content, BorderLayout.NORTH) }
        unsubscribe = service.subscribeStatus { updateStatusLabel() }
        reset()
        updateStatusLabel()
        return panel!!
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return automaticSync?.isSelected != (state.mode == SynchronizationMode.ENABLED) ||
            sourcePath?.text?.trim() != state.palettePath
    }

    override fun apply() {
        service.updateConfiguration(automaticSync?.isSelected == true, sourcePath?.text.orEmpty())
    }

    override fun reset() {
        automaticSync?.isSelected = settings.state.mode == SynchronizationMode.ENABLED
        sourcePath?.text = settings.state.palettePath
        updateStatusLabel()
    }

    override fun disposeUIResources() {
        unsubscribe?.invoke()
        unsubscribe = null
        panel = null
        automaticSync = null
        sourcePath = null
        statusLabel = null
    }

    private fun updateStatusLabel() {
        val status = service.currentStatus()
        statusLabel?.text = "${status.kind.label}: ${status.message}"
    }
}
