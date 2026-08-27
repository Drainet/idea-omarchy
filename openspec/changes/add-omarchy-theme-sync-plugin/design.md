## Context

The workspace contains no plugin implementation yet. Omarchy stores its active colors in `~/.local/state/omarchy/current/theme/colors.toml`; its theme setter builds a new directory and replaces `current/theme` rather than editing a palette in place. The planned behavior is defined by `proposal.md` and the three capability specs.

The plugin is initially for IntelliJ IDEA on Linux systems running Omarchy. It must support both Omarchy light and dark palettes, refresh a running IDE, and leave the user's appearance intact on source failures. JetBrains' theme format supports a base theme plus UI color overrides and a linked editor scheme, but normal theme providers are resource-backed; runtime synchronization requires producing and installing an equivalent runtime appearance.

## Goals / Non-Goals

**Goals:**

- Scaffold a maintainable Kotlin IntelliJ Platform plugin using the current IntelliJ Platform Gradle Plugin and a documented IntelliJ IDEA baseline.
- Build a complete enough palette mapping for coherent standard IDE UI and editor color rendering from arbitrary valid Omarchy palettes.
- Make system-theme updates fast, atomic from the user's perspective, reversible, and safe during failed reads.

**Non-Goals:**

- Bundling an IntelliJ theme for every named Omarchy theme or matching Omarchy wallpaper, icons, fonts, terminal styling, or VS Code-specific overrides.
- Changing Omarchy's active theme, executing Omarchy commands, or supporting non-Omarchy desktop environments in the initial release.
- Maintaining language-specific syntax palettes beyond standard IntelliJ language-default keys in the initial release.
- Guaranteeing every third-party plugin UI component is color-customizable; components without recognized IntelliJ UI keys retain their base-theme behavior.

## Decisions

### 1. Treat `colors.toml` as the canonical integration contract

The source adapter will default to `$HOME/.local/state/omarchy/current/theme/colors.toml`, parse TOML with a maintained JVM library, and validate required semantic keys and hex colors before exposing an immutable `OmarchyPalette`. `mode`, `background`, `foreground`, `accent`, `selection`, `muted`, the ANSI colors, and their bright variants form the initial required mapping set. Users can explicitly choose an alternate palette file in settings.

The adapter will never run `omarchy theme current` or depend on the display-oriented `theme.name`: colors are sufficient and the file is read-only data.

Alternatives considered:

- Invoke `omarchy theme current` and read a named directory: rejected because user overlays and generated colors live in the current state directory, and process execution adds failure and security surface.
- Read an individual application's generated configuration: rejected because those configs are less stable contracts and do not carry the full semantic palette.

### 2. Monitor the state parent and reconcile after debouncing

An application-level lifecycle service will use the JDK watch service on the configured file's nearest stable parent (for the default, `~/.local/state/omarchy/current`) and schedule a single background reconciliation after a short debounce. It will register watches again after directory replacement, retry boundedly if the path is absent or invalid, hash normalized valid palette content, and send only a changed palette to the UI updater.

The service validates from a newly opened file stream, so `DELETE`/`CREATE` bursts caused by Omarchy's `rm -rf` then `mv` sequence cannot cause an incomplete update. Background I/O, parse, hashing, and retry are isolated from the EDT; watcher disposal is tied to application shutdown.

Alternatives considered:

- Watch only `colors.toml`: rejected because the containing directory is replaced and its watch registration is lost.
- Poll continuously: retained only as a bounded retry fallback; a permanent poll wastes resources and reacts more slowly than file events.
- Add an Omarchy `theme-set` hook: rejected for the first release because it mutates the user's desktop configuration and would not observe custom palette changes outside the standard command.

### 3. Generate one semantic model, then render UI and editor outputs from it

`OmarchyPalette` will provide named colors and contrast-derived fallbacks. A `ThemeMapper` will produce (a) a runtime UI-theme document extending IntelliJ Light or Darcula according to `mode`, and (b) a runtime editor scheme based on the matching built-in scheme. The mapper will centrally define the UI-key and editor-key table, rather than scatter raw key names through services. The table covers panels, inputs, menus, trees, tabs, tool windows, status bars, notifications, focus/selection borders, diff/console views, and accessible contrast fallbacks. The editor mapping includes general colors, markup, standard `DefaultLanguageHighlighterColors`, console, diff, and scrollbar keys.

The implementation will load the generated UI theme through the IntelliJ Platform theme loader and install its `LookAndFeelInfo` through `LafManager`; it will install the generated global `EditorColorsScheme` through `EditorColorsManager`. On the EDT, it will update the look-and-feel and repaint/update open windows after both artifacts are ready. Implementation will use the public API where available; any unavoidable platform API used for runtime theme construction will be isolated behind one compatibility adapter and covered by Plugin Verifier for the supported baseline.

Alternatives considered:

- Ship a static `themeProvider` JSON plus static `.icls`: rejected because packaged resources cannot represent user-created palettes or hot-reload the current Omarchy theme.
- Switch only between IntelliJ Light and Darcula and recolor the editor: rejected because it fails the full-IDE appearance requirement.
- Directly mutate Swing `UIManager` defaults: rejected because it bypasses JetBrains look-and-feel lifecycle and is brittle across IDE updates.

### 4. Use an explicit synchronization state machine

The persistent settings state will contain `enabled | paused`, source path, last applied content hash, and an appearance snapshot captured once when synchronization transitions from inactive to enabled. The snapshot stores identifiers (not theme bytes) for both prior UI look-and-feel and editor scheme. A palette update replaces the generated artifacts but never overwrites the snapshot.

Manual appearance or editor-scheme selection observed through IntelliJ's appearance/scheme listeners transitions `enabled` to `paused`, so the plugin never fights the user. Disable/restore stops the watcher, restores the snapshot component-by-component when available, clears only the transient generated runtime artifacts, and preserves the configured path for later use. The Apply action performs a one-shot update while paused.

Alternatives considered:

- Always reapply after any user change: rejected because it makes Appearance settings unusable and hides user intent.
- Omit restoration: rejected because a system integration must not strand users in a generated theme when its source becomes unavailable.

### 5. Target IntelliJ IDEA first and verify compatibility as a release gate

The Gradle project will target a current stable IntelliJ IDEA Community platform baseline chosen during implementation, declare Linux/Omarchy support in its documentation, and test its plugin artifact with the IntelliJ Plugin Verifier against that baseline and the current supported IntelliJ IDEA release. It will avoid product-specific APIs so expansion to other JetBrains IDEs remains possible after compatibility testing.

Alternatives considered:

- Declare all JetBrains IDEs immediately: rejected because theme and bundled-plugin differences need product-by-product verifier and visual validation.
- Pin a very old platform for broad compatibility: rejected because runtime theme support and test infrastructure should begin on a well-supported baseline.

## Risks / Trade-offs

- [The IntelliJ Platform's runtime theme-construction API changes between releases] → Isolate it in a small adapter, pin and document the baseline, run Plugin Verifier in CI, and treat compatibility failures as release blockers.
- [Omarchy's state layout or palette vocabulary evolves] → Centralize source paths, validate a minimum semantic contract, expose the alternate-path setting, and fail without touching the current appearance.
- [Some UI keys vary by platform version or third-party plugin] → Use documented keys where possible, preserve the base theme for unknown keys, and test the standard UI visually on both light and dark palettes.
- [A light palette has insufficient contrast for an IDE control] → calculate contrast for mapper fallbacks and use base-theme contrast-safe values when palette pairs fail the threshold.
- [A theme switch produces several filesystem events] → debounce, hash palettes, serialize reconciliation, and apply only a fully validated changed palette.
- [A crash occurs while a generated theme is active] → persist the pre-sync snapshot before the first update and restore it on the next startup if synchronization is disabled or cannot initialize.

## Migration Plan

1. Publish the first version as an unsigned local ZIP during development and install it into a sandbox IntelliJ IDEA instance.
2. Validate startup, dark-to-dark, dark-to-light, light-to-dark, malformed-source, manual-override, disable/restore, and IDE-restart paths against real Omarchy themes.
3. Run unit/integration tests and Plugin Verifier; package and sign only after all release gates pass.
4. Roll back by disabling or uninstalling the plugin. If it remains active, use Restore Previous Appearance; its saved snapshot is applied on the next valid startup if direct restoration failed.
