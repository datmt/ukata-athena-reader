# Athena Reader

A local-first reading and learning app for the **Linux desktop**: Kotlin/JVM with Compose Multiplatform Desktop, built with the Kotlin Toolchain (Amper). The app is open source (Apache-2.0). An optional paid hosted AI service, Athena AI, is specified separately.

- Product spec: `docs/prd.md`. It is the source of truth for scope, data model, and milestones.
- Hosted AI service spec: `docs/athena-ai-service.md`.

## Commands

The build uses the `./kotlin` wrapper. There is no Gradle.

```sh
./kotlin build                 # build all modules
./kotlin test                  # run all tests
./kotlin test -m shared        # test one module
./kotlin run -m desktopApp     # launch the desktop app
./kotlin show modules          # list modules
```

## Hard constraints

- **No bundled browser engine.** No Chromium, CEF/JCEF/KCEF, or Electron. Render everything natively in Compose (PRD §8.2).
- **Gradle plugins are unavailable** (Amper). Use KSP processors or plain libraries. The database is Room with the bundled SQLite driver, not SQLDelight.
- **No AGPL dependencies** (e.g. MuPDF). Dependencies must be compatible with Apache-2.0.
- **Local-first:** the app works fully offline and without an account. An account exists only for Athena AI.
- **Privacy:** never log book text, notes, AI prompts or responses, or secrets. Secrets go only in the OS keyring.
- **Resource targets** (PRD §10): installer ≤ 120 MB, idle memory ≤ 250 MB, ≤ 400 MB while reading, ~0% CPU when idle.

## Portability rules (PRD §8.3.1)

The MVP ships on Linux only, but code must stay portable to macOS and Windows:

1. OS services (secrets, notifications, file dialogs, appearance, app directories, file associations, opening URLs) sit behind interfaces in `shared`. Linux implementations live in a platform package.
2. No D-Bus, XDG, `/proc`, shell commands, or `xdg-open` in feature or domain code.
3. Use `java.nio.file.Path` for paths. Never concatenate with `/`, never hard-code `~` or `/home`. Stored paths are relative to the data directory.
4. Shortcuts use a platform primary modifier (Ctrl/Cmd), never hard-coded Ctrl.
5. Load native libraries through one OS/architecture-aware loader. No native dependencies without macOS and Windows builds unless the decision is recorded.
6. Bundle fonts for reading content.
7. Read and write UTF-8 explicitly. Write `\n` line endings, and accept `\r\n` when reading.
8. Tray, single-instance, and window behaviour live in `desktopApp`, not in feature modules.
9. Packaging scripts go under `packaging/<os>/`.
10. Prefer pure-JVM or cross-platform libraries (e.g. `java-keyring` over raw D-Bus bindings).

## Code layout

- `shared/src`: application code (domain, data, AI, UI). `shared/src@jvm`: JVM-specific code.
- `desktopApp/src`: entry point and window/process behaviour.
- The planned module split is in PRD §8.3. `core-domain` depends on nothing, and feature modules depend only on domain interfaces.
