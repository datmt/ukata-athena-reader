# Progress

## Milestone 0 — Technical spike (`spike/`, throwaway)

| # | Spike | Status |
|---|---|---|
| 1 | Cross-block text selection | model + tests done; Compose layer compiles, awaiting human feel-check |
| 2 | PDFium via FFM (linux-arm64) | todo |
| 3 | Native EPUB layout | todo |
| 4 | Mermaid subset (ELK vs in-house) | todo |
| 5 | JLaTeXMath | todo |
| 6 | Room + FTS5 | todo |
| 7 | AppImage/.deb + resource targets | todo |
| 8 | Wayland/X11 + HiDPI | human check |

Results and decisions: `spike/RESULTS.md`.

### Log
- Spike 1: `SelectionModel` (pure Kotlin: anchors, word/paragraph, cross-block text, per-block ranges), 9 unit tests green. `SelectableColumn`: single pointer handler, hit-test over visible `LazyColumn` items, double/triple click, shift-extend, drag auto-scroll, per-block highlight via `getPathForRange`. Run: `./kotlin run -m spike`.
