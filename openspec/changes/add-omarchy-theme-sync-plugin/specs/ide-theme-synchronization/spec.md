## Purpose

Keep IntelliJ IDEA's application UI and editor scheme aligned with Omarchy's active semantic color palette while preserving a safe path back to the user's original appearance.

## ADDED Requirements

### Requirement: Apply an Omarchy-derived IDE appearance
When synchronization is enabled and a valid palette is available, the plugin SHALL apply an IntelliJ IDEA appearance derived from that palette. The appearance SHALL use the palette mode to select a matching light or dark base and shall apply palette-derived colors to the standard IDE surface, text, selection, focus, tab, menu, and status controls supported by the target IntelliJ Platform.

#### Scenario: Dark Omarchy palette becomes active
- **WHEN** synchronization is enabled and the active palette declares dark mode
- **THEN** IntelliJ IDEA uses an Omarchy-derived dark application appearance with the palette's background, foreground, accent, selection, and muted colors reflected in supported UI controls

#### Scenario: Light Omarchy palette becomes active
- **WHEN** synchronization is enabled and the active palette declares light mode
- **THEN** IntelliJ IDEA uses an Omarchy-derived light application appearance with the corresponding palette colors reflected in supported UI controls

### Requirement: Apply a matching editor color scheme
The plugin SHALL apply an editor scheme derived from the same validated palette as the application appearance. The scheme SHALL set editor background, default text, caret, selection, line numbers, whitespace, matching-brace, search-result, diff, console, and standard language-default syntax colors using the palette's semantic colors.

#### Scenario: Palette update changes editor colors
- **WHEN** a new valid Omarchy palette is applied
- **THEN** open editors and newly opened editors display the generated scheme's updated background, text, selection, caret, and syntax colors

#### Scenario: Editor language has no specific mapping
- **WHEN** an editor language has no language-specific color override in the generated scheme
- **THEN** it inherits the generated standard language-default syntax colors rather than falling back to an unrelated IDE scheme

### Requirement: Update the UI and editor together
For each newly accepted palette, the plugin SHALL install the generated application appearance and matching editor scheme as one logical update on the IDE user-interface thread. It SHALL refresh all open windows and editors after installation.

#### Scenario: System theme changes while IDEA is open
- **WHEN** Omarchy changes from one valid palette to another while IntelliJ IDEA is running
- **THEN** all open IDE windows and editor tabs update to the new application appearance and matching editor scheme without requiring an IDE restart

### Requirement: Preserve and restore the prior user appearance
On enabling synchronization, the plugin SHALL record the user's current application appearance and global editor scheme once. On disabling synchronization or selecting Restore Previous Appearance, it SHALL stop automatic updates and restore that recorded appearance and scheme when they remain available.

#### Scenario: User disables synchronization
- **WHEN** the user disables synchronization after an Omarchy appearance has been applied
- **THEN** the plugin restores the appearance and editor scheme that were active immediately before synchronization was enabled

#### Scenario: Recorded appearance is no longer available
- **WHEN** synchronization is disabled and the recorded appearance or scheme has been uninstalled or deleted
- **THEN** the plugin keeps the current appearance, restores every remaining recorded component, and reports the component that could not be restored

### Requirement: Respect a manual appearance choice
The plugin SHALL stop synchronization when the user selects a different application appearance or editor scheme outside the plugin while synchronization is active. It SHALL leave that manually selected appearance in effect and report that synchronization was paused by a manual change.

#### Scenario: User selects another IDE theme
- **WHEN** synchronization is active and the user selects a non-Omarchy IDE appearance in IntelliJ settings
- **THEN** the plugin preserves the user's selection and changes synchronization status to paused
