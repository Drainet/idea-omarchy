# Omarchy Theme Sync

An IntelliJ IDEA plugin that keeps the IDE UI and editor scheme aligned with the
active Omarchy palette. It reads only the local Omarchy state file at
`~/.local/state/omarchy/current/theme/colors.toml`; it never runs Omarchy
commands, changes system configuration, or sends theme data over the network.

## Development baseline

The initial baseline is IntelliJ IDEA 2026.2 (build 262) on Linux, built with
Java 25 and Gradle 9. IntelliJ IDEA has used the unified distribution since
2025.3, so the project targets unified IDEA rather than the retired
Community-only artifact. Development and verification use IDEA 2026.2.1
(build `IU-262.9437.185`).

## Build

```sh
export JAVA_HOME=/path/to/idea/jbr # Java 25
./gradlew test
./gradlew buildPlugin
```

The resulting plugin ZIP is written under `build/distributions/`.

## Installation and rollback

Install the ZIP in **Settings | Plugins | Install Plugin from Disk**. Configure
the palette source in **Settings | Appearance & Behavior | Omarchy Theme Sync**.
Use **Tools | Restore Previous Appearance** to immediately return to the UI
theme and editor scheme that were active before synchronization. Disabling or
uninstalling the plugin also stops monitoring; if a saved theme no longer
exists, the plugin leaves the remaining active component intact and explains
what could not be restored.

The available controls are:

- **Settings | Appearance & Behavior | Omarchy Theme Sync** — enable automatic
  synchronization and optionally select another `colors.toml` source.
- **Tools | Enable Omarchy Theme Synchronization**, **Pause**, **Apply Current
  Omarchy Theme**, and **Restore Previous Appearance**.
- The status-bar widget reports paused, waiting, unavailable, synchronized, or
  application-failure state.

## Privacy and permissions

The plugin reads `colors.toml` locally and watches its containing state
directory. It never executes Omarchy commands, writes Omarchy files, or sends
the palette, filesystem path, theme name, or derived colors over the network.
The only persistent data is the selected source path, synchronization mode,
last successful palette fingerprint, and the identifiers required to restore
the previous IntelliJ appearance.

## Validation

The automated suite covers dark and light fixtures, malformed or missing
palette input, RGB/RGBA and contrast handling, normalized content hashing,
unchanged-file suppression, and replacement of a watched theme directory.

The 2026.2.1 sandbox smoke test loaded the plugin at startup, read the active
Omarchy palette, and persisted its fingerprint with the generated **Omarchy
System** editor scheme selected. Plugin Verifier reports the packaged plugin
as compatible with `IU-262.9437.185`.

Before publishing beyond this local baseline, manually check a live sandbox
for dark-to-light and light-to-dark changes, manual IDE theme selection,
invalid-palette recovery, and restore behavior.

## Troubleshooting

- Check that `colors.toml` exists and contains a complete Omarchy palette.
- Use the settings page to choose another palette file when Omarchy state is in
  a non-default location.
- The plugin waits for a complete valid file during a system-theme transition;
  it preserves the last working IDE appearance if the source is briefly absent
  or malformed.
