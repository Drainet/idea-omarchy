## Why

Omarchy switches a complete system palette, but IntelliJ IDEA remains on a manually selected JetBrains theme and editor scheme. A plugin that derives its UI and editor colors from Omarchy's active palette will keep the development environment visually coherent without per-theme IDE configuration.

## What Changes

- Add an IntelliJ Platform plugin, initially targeting IntelliJ IDEA on Omarchy/Linux.
- Read the active Omarchy palette from `~/.local/state/omarchy/current/theme/colors.toml` and use its `mode` and semantic color keys to create an Omarchy-backed IDE appearance and editor color scheme.
- Detect Omarchy theme replacement while the IDE is running, re-read the palette only after a complete valid update is available, and refresh the UI and editor scheme automatically.
- Provide a settings page and actions to enable or disable synchronization, choose the palette source, apply it immediately, and restore the IDE appearance that was active before synchronization.
- Make invalid, unavailable, or unsupported Omarchy palette data non-destructive: preserve the last working IDE appearance and surface an actionable status message.

## Capabilities

### New Capabilities

- `omarchy-theme-source`: Discover, validate, and monitor Omarchy's active theme palette safely.
- `ide-theme-synchronization`: Apply and restore an Omarchy-derived IntelliJ UI theme and editor scheme, including automatic updates.
- `theme-sync-controls`: Expose user controls, status, and recovery behavior for synchronization.

### Modified Capabilities

- None.

## Impact

- Adds a Kotlin/Gradle IntelliJ Platform plugin project, plugin metadata, settings UI, and automated tests.
- Integrates with IntelliJ Platform look-and-feel and editor-color-scheme APIs; compatibility must be validated against the selected IntelliJ IDEA baseline.
- Reads only the user's Omarchy state under `~/.local/state/omarchy/current/`; it neither invokes Omarchy commands nor modifies Omarchy configuration.
