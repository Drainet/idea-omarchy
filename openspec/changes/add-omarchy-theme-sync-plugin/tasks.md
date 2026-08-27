## 1. Bootstrap the IntelliJ Platform plugin

- [x] 1.1 Create the Kotlin/Gradle IntelliJ Platform plugin project, select and document a supported unified IntelliJ IDEA baseline, and verify `./gradlew verifyPluginProjectConfiguration` succeeds.
- [x] 1.2 Configure plugin identity, Linux/Omarchy compatibility metadata, application services, settings configurable, and action registrations; verify the sandbox IDE loads the plugin without descriptor errors.
- [ ] 1.3 Add a maintained TOML parser and the test/tooling dependencies, including IntelliJ Plugin Verifier support; verify dependency resolution and the test task both succeed from a clean Gradle cache.

## 2. Read and monitor Omarchy palettes

- [x] 2.1 Implement immutable palette, source-path, parse-result, and diagnostic models; verify unit tests cover dark and light real `colors.toml` fixtures.
- [x] 2.2 Implement TOML parsing and semantic validation for mode, required palette entries, RGB/RGBA syntax, regular-file checks, and alternate paths; verify tests reject missing, malformed, unreadable, and unsupported input without returning a palette.
- [x] 2.3 Implement normalized palette hashing and source reconciliation so unchanged content does not trigger an update; verify unit tests produce one update for a changed palette and none for equivalent formatting or unrelated events.
- [x] 2.4 Implement a disposable, debounced filesystem monitor for the default Omarchy state parent and configured alternate paths, including bounded retry and watch re-registration; verify integration tests replace a temporary `theme` directory and observe exactly one valid palette notification.
- [ ] 2.5 Add startup source availability handling and diagnostics; verify a missing source leaves a test appearance coordinator untouched and reports the unavailable state.

## 3. Generate the Omarchy UI theme and editor scheme

- [x] 3.1 Implement color conversion, luminance/contrast checks, and semantic fallback rules; verify unit tests cover valid RGB/RGBA conversion and contrast-safe light and dark fallback choices.
- [ ] 3.2 Implement a centralized UI-key mapping that derives the supported standard IDE surfaces, controls, menus, tabs, tool windows, status UI, focus, selection, notification, diff, and console colors from the palette; verify snapshot tests assert the generated light and dark runtime theme documents against fixtures.
- [ ] 3.3 Isolate runtime IntelliJ UI-theme loading and look-and-feel installation behind a compatibility adapter; verify an IDE integration test installs the generated light and dark appearances, updates/repaints UI, and Plugin Verifier reports no baseline compatibility problem.
- [ ] 3.4 Implement the generated editor scheme from the corresponding built-in base, including editor basics, markup, standard language-default syntax, console, diff, scrollbar, caret, and selection colors; verify scheme tests assert all required color/text-attribute keys and inheritance behavior.
- [ ] 3.5 Implement an EDT-only atomic appearance applier that installs the matching UI and editor artifacts together; verify an IDE integration test updates existing and newly created editor instances without restarting the IDE.

## 4. Coordinate synchronization and restoration

- [ ] 4.1 Implement persistent plugin settings for mode (`enabled` or `paused`), source path, palette identity, and prior UI/editor appearance identifiers without persisting palette contents; verify serialization round-trip tests and inspect persisted state for the absence of colors.
- [ ] 4.2 Implement the synchronization state machine, capturing the pre-sync appearance exactly once, applying only valid changed palettes, and retaining the last working appearance when reads fail; verify state-transition tests for enable, retry, update, pause, disable, and restart.
- [ ] 4.3 Implement restoration of prior UI and editor appearance independently with clear partial-restoration diagnostics; verify tests cover complete restoration and a missing saved theme or editor scheme.
- [ ] 4.4 Observe external look-and-feel and global editor-scheme changes, distinguish plugin-originated updates, and pause synchronization on a manual selection; verify an IDE integration test keeps the manually selected appearance after an Omarchy file event.
- [ ] 4.5 Wire lifecycle startup, shutdown, watcher disposal, and recovery behavior into the application service; verify repeated sandbox IDE open/close runs produce no leaked watcher or retry threads.

## 5. Add user controls and feedback

- [ ] 5.1 Implement the settings UI for synchronization state and source path, with inline validation, status, and recovery guidance; verify UI tests cover valid, invalid, unavailable, synchronized, and paused states.
- [ ] 5.2 Implement Enable/Resume, Pause, Apply Current Omarchy Theme, and Restore Previous Appearance actions; verify action tests confirm one-shot apply remains paused and restore stops monitoring.
- [ ] 5.3 Add a concise status indicator/notification flow that reports source and application failures without repeatedly notifying during the same invalid transition; verify notification-deduplication tests and a sandbox smoke test.

## 6. Validate, document, and package

- [ ] 6.1 Add unit and IDE integration coverage for all scenarios in `omarchy-theme-source`, `ide-theme-synchronization`, and `theme-sync-controls`; verify `./gradlew test` passes.
- [x] 6.2 Run IntelliJ Plugin Verifier against the documented baseline and current supported IntelliJ IDEA release; verify the verification report contains no compatibility errors.
- [ ] 6.3 Perform sandbox and real-Omarchy smoke tests for startup, dark-to-dark, dark-to-light, light-to-dark, directory replacement, invalid palette recovery, manual override, disable/restore, and restart; verify the checklist is recorded in the project documentation.
- [x] 6.4 Write installation, permissions/privacy, supported-platform, troubleshooting, and rollback documentation; verify `./gradlew buildPlugin` produces an installable ZIP referenced by the README.
