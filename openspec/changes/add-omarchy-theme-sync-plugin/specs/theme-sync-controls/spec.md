## Purpose

Give IntelliJ IDEA users explicit control and clear status for the Omarchy theme synchronization lifecycle and recovery paths.

## ADDED Requirements

### Requirement: Provide synchronization controls
The plugin SHALL provide a discoverable settings page and IDE actions to enable synchronization, pause or resume synchronization, apply the current palette immediately, restore the prior appearance, and configure the palette source path.

#### Scenario: User enables synchronization from settings
- **WHEN** the user enables synchronization in the plugin settings and applies the change
- **THEN** the plugin validates the configured palette and applies it immediately when valid

#### Scenario: User requests immediate apply
- **WHEN** the user invokes the Apply Current Omarchy Theme action while synchronization is paused
- **THEN** the plugin validates and applies the current palette once without resuming automatic updates

### Requirement: Persist user preferences and recovery data
The plugin SHALL persist the synchronization state, configured palette source, last successful palette identity, and prior appearance needed for restoration across IDE restarts. It SHALL not persist the full contents of the user's palette.

#### Scenario: IDE restarts with synchronization enabled
- **WHEN** IntelliJ IDEA restarts while synchronization is enabled
- **THEN** the plugin validates the active palette at startup and applies it when valid

#### Scenario: IDE restarts after synchronization was paused
- **WHEN** IntelliJ IDEA restarts while synchronization is paused
- **THEN** the plugin does not alter the user's active IDE appearance automatically

### Requirement: Expose accurate synchronization status
The settings page and status indicator SHALL identify whether synchronization is enabled, paused, unavailable, waiting for valid palette data, or successfully synchronized. For unsuccessful validation or application attempts, the plugin SHALL provide a concise diagnostic and a recovery action where one exists.

#### Scenario: Invalid configured source is corrected
- **WHEN** synchronization is waiting because the configured palette path is invalid and the user corrects it to a valid palette
- **THEN** the status changes to synchronized after the palette is applied

### Requirement: Keep palette data private
The plugin SHALL process palette data locally and SHALL not transmit the palette file, theme name, filesystem path, or derived color values over the network.

#### Scenario: Synchronization runs normally
- **WHEN** the plugin reads and applies an Omarchy palette
- **THEN** no network request is made for palette discovery, parsing, monitoring, or application
