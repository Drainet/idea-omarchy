## Purpose

Provide a reliable, read-only source of the active Omarchy palette so IDE appearance synchronization can follow every system theme change safely.

## ADDED Requirements

### Requirement: Read the active Omarchy palette
The plugin SHALL read the default active-palette file at `~/.local/state/omarchy/current/theme/colors.toml`, unless the user has configured an alternate palette path. It SHALL obtain the theme mode and the semantic palette entries needed for IDE and editor rendering from the same valid file snapshot.

#### Scenario: Default Omarchy palette is available
- **WHEN** the default Omarchy `colors.toml` contains a supported `mode` and valid palette values
- **THEN** the plugin makes the parsed palette available for synchronization without invoking an Omarchy command or modifying an Omarchy file

#### Scenario: Alternate palette path is configured
- **WHEN** the user configures a readable alternate `colors.toml` path containing a supported palette
- **THEN** the plugin uses that palette instead of the default Omarchy state path

### Requirement: Validate palette input before use
The plugin SHALL accept only a palette with `mode = "dark"` or `mode = "light"` and valid RGB or RGBA hexadecimal values for all colors required by its generated theme. It SHALL reject malformed, incomplete, unreadable, or non-regular palette files.

#### Scenario: Palette contains invalid color data
- **WHEN** a required palette value is malformed or missing
- **THEN** the plugin does not replace the current IDE appearance with data from that palette and reports that synchronization is waiting for a valid palette

#### Scenario: Palette source is unavailable at startup
- **WHEN** no readable valid palette is available when the IDE starts
- **THEN** the plugin leaves the existing IDE appearance unchanged and exposes the unavailable source status

### Requirement: Follow atomic Omarchy theme replacement
The plugin SHALL detect replacement, creation, deletion, and modification events affecting the configured palette or its parent state directory. It SHALL coalesce a burst of events and retry reading until a complete valid palette is available, so it never intentionally applies a partially written theme.

#### Scenario: Omarchy replaces the current theme directory
- **WHEN** Omarchy atomically replaces `~/.local/state/omarchy/current/theme/` during a theme switch
- **THEN** the plugin discovers the new valid palette and schedules exactly one appearance update for the resulting palette

#### Scenario: Theme transition temporarily removes the palette
- **WHEN** the configured palette is temporarily absent while Omarchy switches themes
- **THEN** the plugin retains the last successfully applied appearance and resumes synchronization when a valid replacement appears

### Requirement: Avoid redundant palette updates
The plugin SHALL identify the content of the last successfully applied palette and SHALL not reapply the IDE appearance when a filesystem event leaves that content unchanged.

#### Scenario: Non-palette file changes within the watched state
- **WHEN** an Omarchy state event occurs but the valid palette content is unchanged
- **THEN** the plugin does not refresh the IDE appearance
