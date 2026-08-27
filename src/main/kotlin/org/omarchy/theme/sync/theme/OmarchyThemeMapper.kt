package org.omarchy.theme.sync.theme

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import org.omarchy.theme.sync.model.OmarchyPalette
import org.omarchy.theme.sync.model.PaletteMode
import org.omarchy.theme.sync.model.RgbaColor
import java.awt.Font

data class RuntimeThemeDocument(
    val name: String,
    val json: String,
)

object OmarchyThemeMapper {
    const val THEME_NAME = "Omarchy System"
    const val SCHEME_NAME = "Omarchy System"

    fun uiTheme(palette: OmarchyPalette): RuntimeThemeDocument {
        val namedColors = linkedMapOf(
            "background" to palette["background"],
            "foreground" to readableForeground(palette["foreground"], palette["background"], palette.mode),
            "accent" to palette["accent"],
            "selection" to palette["selection"],
            "muted" to palette["muted"],
            "red" to palette["red"],
            "yellow" to palette["yellow"],
            "orange" to palette["orange"],
            "green" to palette["green"],
            "cyan" to palette["cyan"],
            "blue" to palette["blue"],
            "magenta" to palette["magenta"],
            "brightForeground" to readableForeground(palette["bright_cyan"], palette["background"], palette.mode),
        )
        val ui = linkedMapOf(
            "*" to mapOf("background" to "background", "foreground" to "foreground"),
            "Panel.background" to "background",
            "Panel.foreground" to "foreground",
            "Label.foreground" to "foreground",
            "Component.borderColor" to "muted",
            "Component.focusedBorderColor" to "accent",
            "TextField.background" to "background",
            "TextField.foreground" to "foreground",
            "TextField.selectionBackground" to "selection",
            "TextField.selectionForeground" to "foreground",
            "ComboBox.background" to "background",
            "ComboBox.foreground" to "foreground",
            "Button.startBackground" to "selection",
            "Button.endBackground" to "selection",
            "Button.foreground" to "foreground",
            "Menu.background" to "background",
            "Menu.foreground" to "foreground",
            "MenuItem.background" to "background",
            "MenuItem.foreground" to "foreground",
            "MenuItem.selectionBackground" to "selection",
            "MenuItem.selectionForeground" to "foreground",
            "Tree.background" to "background",
            "Tree.foreground" to "foreground",
            "Tree.selectionBackground" to "selection",
            "Tree.selectionForeground" to "foreground",
            "List.background" to "background",
            "List.foreground" to "foreground",
            "List.selectionBackground" to "selection",
            "List.selectionForeground" to "foreground",
            "Table.background" to "background",
            "Table.foreground" to "foreground",
            "Table.selectionBackground" to "selection",
            "Table.selectionForeground" to "foreground",
            "EditorTabs.background" to "background",
            "EditorTabs.selectedBackground" to "selection",
            "EditorTabs.selectedForeground" to "foreground",
            "EditorTabs.underlineColor" to "accent",
            "ToolWindow.background" to "background",
            "StatusBar.background" to "background",
            "StatusBar.foreground" to "foreground",
            "Notification.background" to "selection",
            "Notification.foreground" to "foreground",
            "Link.activeForeground" to "accent",
            "Link.hoverForeground" to "brightForeground",
        )

        val json = buildString {
            append("{\n")
            append("  \"name\": \"").append(THEME_NAME).append("\",\n")
            append("  \"author\": \"Omarchy Theme Sync\",\n")
            append("  \"parentTheme\": \"").append(if (palette.mode == PaletteMode.DARK) "Darcula" else "IntelliJLight").append("\",\n")
            append("  \"dark\": ").append(palette.mode == PaletteMode.DARK).append(",\n")
            append("  \"colors\": ").append(jsonObject(namedColors.mapValues { it.value.hex })).append(",\n")
            append("  \"ui\": ").append(jsonObject(ui)).append("\n")
            append('}')
        }
        return RuntimeThemeDocument(THEME_NAME, json)
    }

    fun editorScheme(palette: OmarchyPalette, baseScheme: EditorColorsScheme): EditorColorsScheme {
        val scheme = baseScheme.clone() as EditorColorsScheme
        scheme.name = SCHEME_NAME
        val background = palette["background"].toAwtColor()
        val foreground = readableForeground(palette["foreground"], palette["background"], palette.mode).toAwtColor()
        val selection = palette["selection"].toAwtColor()
        val muted = palette["muted"].toAwtColor()

        scheme.setColor(EditorColors.CARET_ROW_COLOR, palette["selection"].toAwtColor())
        scheme.setColor(EditorColors.CARET_COLOR, palette["accent"].toAwtColor())
        scheme.setColor(EditorColors.LINE_NUMBERS_COLOR, muted)
        scheme.setColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR, foreground)
        scheme.setColor(EditorColors.WHITESPACES_COLOR, muted)
        scheme.setColor(EditorColors.TABS_COLOR, muted)
        scheme.setColor(EditorColors.INDENT_GUIDE_COLOR, muted)
        scheme.setColor(EditorColors.RIGHT_MARGIN_COLOR, muted)
        scheme.setColor(EditorColors.SELECTION_BACKGROUND_COLOR, selection)
        scheme.setColor(ColorKey.createColorKey("SELECTION_BACKGROUND_COLOR_INACTIVE"), selection)
        scheme.setColor(EditorColors.SELECTION_FOREGROUND_COLOR, foreground)
        scheme.setColor(ColorKey.createColorKey("SCROLLBAR_THUMB_COLOR"), palette["accent"].toAwtColor())
        scheme.setColor(EditorColors.EDITOR_GUTTER_BACKGROUND, background)
        scheme.setColor(EditorColors.GUTTER_BACKGROUND, background)
        scheme.setColor(EditorColors.NOTIFICATION_BACKGROUND, selection)
        scheme.setColor(EditorColors.ADDED_LINES_COLOR, palette["green"].toAwtColor())
        scheme.setColor(EditorColors.MODIFIED_LINES_COLOR, palette["blue"].toAwtColor())
        scheme.setColor(EditorColors.DELETED_LINES_COLOR, palette["red"].toAwtColor())

        setText(scheme, TextAttributesKey.createTextAttributesKey("TEXT"), foreground, background)
        setText(scheme, EditorColors.SEARCH_RESULT_ATTRIBUTES, foreground, palette["yellow"].toAwtColor())
        setText(scheme, EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES, foreground, palette["yellow"].toAwtColor())
        setText(scheme, EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES, foreground, palette["orange"].toAwtColor())
        setText(scheme, EditorColors.REFERENCE_HYPERLINK_COLOR, palette["accent"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.IDENTIFIER, foreground)
        setText(scheme, DefaultLanguageHighlighterColors.KEYWORD, palette["magenta"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.STRING, palette["green"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.NUMBER, palette["orange"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.LINE_COMMENT, muted)
        setText(scheme, DefaultLanguageHighlighterColors.BLOCK_COMMENT, muted)
        setText(scheme, DefaultLanguageHighlighterColors.DOC_COMMENT, muted)
        setText(scheme, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION, palette["blue"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.FUNCTION_CALL, palette["blue"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.CLASS_NAME, palette["yellow"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.INTERFACE_NAME, palette["yellow"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.CONSTANT, palette["cyan"].toAwtColor())
        setText(scheme, DefaultLanguageHighlighterColors.PARAMETER, palette["foreground"].toAwtColor())
        return scheme
    }

    private fun setText(scheme: EditorColorsScheme, key: TextAttributesKey, foreground: java.awt.Color, background: java.awt.Color? = null) {
        scheme.setAttributes(key, TextAttributes(foreground, background, null, null, Font.PLAIN))
    }

    private fun readableForeground(candidate: RgbaColor, background: RgbaColor, mode: PaletteMode): RgbaColor =
        if (candidate.contrastRatio(background) >= 4.5) candidate else if (mode == PaletteMode.DARK) WHITE else BLACK

    private fun jsonObject(values: Map<String, *>): String = values.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "\"${escape(key)}\":${jsonValue(value)}"
    }

    private fun jsonValue(value: Any?): String = when (value) {
        is String -> "\"${escape(value)}\""
        is Map<*, *> -> jsonObject(value.entries.associate { it.key.toString() to it.value })
        else -> value.toString()
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private val WHITE = RgbaColor.parse("#ffffff")!!
    private val BLACK = RgbaColor.parse("#000000")!!
}
