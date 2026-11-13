# Progress

## Milestone 0 — Technical spike (`spike/`, throwaway)

| # | Spike | Status |
|---|---|---|
| 1 | Cross-block text selection | model + tests done; Compose layer compiles, awaiting human feel-check |
| 2 | PDFium via FFM (linux-arm64) | done (binding + test green; bundling pending) |
| 3 | Native EPUB layout | parser + view done, tests green; needs real-EPUB check |
| 4 | Mermaid subset (ELK vs in-house) | done: in-house; parser+layout tests green; awaiting human look |
| 5 | JLaTeXMath | done; tests green; awaiting human look |
| 6 | Room + FTS5 | done (linux-arm64; other OS unverified) |
| 7 | AppImage/.deb + resource targets | .deb + tar.gz + AppImage (arm64) built locally; x86_64 AppImage cross-assembled, untested |
| 8 | Wayland/X11 + HiDPI | human check |

Results and decisions: `spike/RESULTS.md`.

### Log
- Spike 1: `SelectionModel` (pure Kotlin: anchors, word/paragraph, cross-block text, per-block ranges), 9 unit tests green. `SelectableColumn`: single pointer handler, hit-test over visible `LazyColumn` items, double/triple click, shift-extend, drag auto-scroll, per-block highlight via `getPathForRange`. Run: `./kotlin run -m spike`.
- Repo published: https://github.com/datmt/ukata-athena-reader (public, `main`). Note: `docs/athena-ai-service.md` is public too; review whether it should be.
- PRD path in `../AGENTS.md` fixed to `athena-reader/docs/prd.md`.

- Spike 2: FFM PDFium binding + test (uses system LibreOffice libpdfiumlo.so, no download). 7 ms render, text + char boxes OK.

- Spike 3: `EpubBook` (JDK-only zip+StAX, lazy chapters, flat block model + spans), `toSpikeBlocks` view, chapter nav in spike app. Run: `./kotlin run -m spike -- book.epub`.

- Spike 6: new throwaway module `spikedb` (Room 2.8.5 + KSP + bundled SQLite). FTS5 external-content table + triggers works, test green.

- Spike 4: in-house `spike/src/mermaid/` (no ELK): flowchart + sequence parsers, Sugiyama-style layout, Compose Canvas `MermaidView`. 6 tests green. Run: `./kotlin run -m spike -- mermaid [file.mmd]`.

- Spike 5: JLaTeXMath 1.0.7 (670 KB jar, only new dep) → ARGB bitmap → Compose `Image` (`spike/src/math/`). 3 tests green, ~0.5 ms/formula warm. Run: `./kotlin run -m spike -- math`.

- Spike 7: `packaging/linux/build.sh` (jar -> jlink -> jpackage app-image -> hand-built .deb + tar.gz), run locally (no CI). arm64 spike app: image 101 MB, .deb 73 MB. Amper executable-jar bug: same-named jars from different groups collide (stub `runtime-desktop` won), script rebuilds lib set. `packaging/linux/appimage.sh <arch>` makes AppImages (aarch64 native: 76 MB, runs to Compose; x86_64 cross-assembled from Temurin x64 JRE + x64 Skiko jar: 95 MB, **untested, needs an x64 host**, un-jlinked so larger).

- Spike launcher: packaged app opens tabs (Selection, EPUB via Open..., Mermaid, Math). Human check of the arm64 AppImage: math and mermaid render OK. **Feedback: text is painfully small** (demo fonts 17sp, no UI scale/HiDPI handling; fix with a reader font-size setting and a density/uiScale check in M1). No library management yet (that is M1, FR-001..006).
- Jenkins build to be set up by the user (x64 AppImage build + smoke test, R2 upload, README/release page deferred until then).

### Next
0. Wrap-up state: M0 spikes 1-7 built and packaged; remaining human checks (1, 3, 8, x64 AppImage), then record M0 go/no-go in RESULTS.md and plan M1 (library: Room schema, import, library UI, reader position) before coding.
1. Human check of spike 1 (`./kotlin run -m spike`; drag, auto-scroll, double/triple/shift-click, `-Dsun.java2d.uiScale=1.5`).
2. Spikes 2, 3, 6 done. Next: packaging (7). Human look at spike 4 and 5 demos.
3. Open questions: test corpus (public-domain vs own files), spike time-box (proposed 1-2 weeks).
