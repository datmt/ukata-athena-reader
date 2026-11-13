# Progress

## Milestone 0 — Technical spike (`spike/`, throwaway)

| # | Spike | Status |
|---|---|---|
| 1 | Cross-block text selection | model + tests done; Compose layer compiles, awaiting human feel-check |
| 2 | PDFium via FFM (linux-arm64) | done (binding + test green; bundling pending) |
| 3 | Native EPUB layout | parser + view done, tests green; needs real-EPUB check |
| 4 | Mermaid subset (ELK vs in-house) | todo |
| 5 | JLaTeXMath | todo |
| 6 | Room + FTS5 | done (linux-arm64; other OS unverified) |
| 7 | AppImage/.deb + resource targets | todo |
| 8 | Wayland/X11 + HiDPI | human check |

Results and decisions: `spike/RESULTS.md`.

### Log
- Spike 1: `SelectionModel` (pure Kotlin: anchors, word/paragraph, cross-block text, per-block ranges), 9 unit tests green. `SelectableColumn`: single pointer handler, hit-test over visible `LazyColumn` items, double/triple click, shift-extend, drag auto-scroll, per-block highlight via `getPathForRange`. Run: `./kotlin run -m spike`.
- Repo published: https://github.com/datmt/ukata-athena-reader (public, `main`). Note: `docs/athena-ai-service.md` is public too; review whether it should be.
- PRD path in `../AGENTS.md` fixed to `athena-reader/docs/prd.md`.

- Spike 2: FFM PDFium binding + test (uses system LibreOffice libpdfiumlo.so, no download). 7 ms render, text + char boxes OK.

- Spike 3: `EpubBook` (JDK-only zip+StAX, lazy chapters, flat block model + spans), `toSpikeBlocks` view, chapter nav in spike app. Run: `./kotlin run -m spike -- book.epub`.

- Spike 6: new throwaway module `spikedb` (Room 2.8.5 + KSP + bundled SQLite). FTS5 external-content table + triggers works, test green.

### Next
1. Human check of spike 1 (`./kotlin run -m spike`; drag, auto-scroll, double/triple/shift-click, `-Dsun.java2d.uiScale=1.5`).
2. Spikes 2, 3, 6 done. Next: Mermaid (4), JLaTeXMath (5), packaging (7).
3. Open questions: test corpus (public-domain vs own files), spike time-box (proposed 1-2 weeks).
