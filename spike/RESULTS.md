# Milestone 0 results

## 1. Cross-block selection
- Design: per-block highlight drawn in each item (`drawBehind`) instead of one global overlay, because LazyColumn only composes visible items; text extraction uses the text-only `SelectionModel`, so off-screen blocks are included.
- Unit-tested: ordering, backward selection, word edges, paragraph, clamp. **Pending human check:** drag feel, auto-scroll, 100/150/200% scale (`-Dsun.java2d.uiScale=1.5`).

## 2. PDFium via FFM (linux-arm64)
- Pure Java 25 FFM binding (`spike/src/pdf/Pdfium.kt`, ~100 lines, no JNI/extra libs): open, page size, BGRA render, text extract, char boxes.
- Works against the arm64 `libpdfiumlo.so` shipped with LibreOffice (full FPDF/FPDFText API). Page 1 of a real spec PDF: 914x1183 render in ~7 ms (warm-ish, single run), text extraction correct.
- Needs `--enable-native-access=ALL-UNNAMED` to silence the warning (set in app JVM args).
- **Open:** for shipping we must bundle our own pdfium-binaries (linux-arm64 + x64); download needs approval. Cold-start and large-page timings not yet measured.

## 3. Native EPUB layout
- Approach: no HTML engine. `EpubBook` (JDK zip + StAX only) parses container/OPF/nav-or-NCX at open; chapters parse lazily into a flat `Block` list (heading/paragraph/list item/quote/pre/image) with inline spans (bold/italic/code/link/sup-sub). Layout is Compose text in the same `LazyColumn` used for selection, so selection, location and highlights work unchanged (location = chapter, block, char offset).
- Handled: percent-encoded/relative hrefs, whitespace collapsing, `<pre>`, HTML named entities without DTD (small map; unknown → space), blockquote nesting, skipped head/script/style/svg/math. 4 unit tests green.
- **Not covered (decide before M1):** images render as `[image: alt]` placeholder; tables flattened to paragraphs; CSS ignored (deliberate: reader controls typography); no fixed-layout/RTL/vertical text; no footnote popups; link navigation not wired; EPUB CFI not implemented (block+offset is our stable location, needs mapping if CFI export is required).
- **Pending human/corpus check:** no real EPUB on this machine; run `./kotlin run -m spike -- path/to/book.epub` with a few real books (novel, technical with code, one with many images).

## 6. Room + FTS5 (module `spikedb`, throwaway)
- **Works on linux-arm64:** Room 2.8.5 (KSP via `settings.kotlin.ksp` in module.yaml, processor `androidx.room:room-compiler`) + `androidx.sqlite:sqlite-bundled:2.6.2`. Schema JSON exported to `spikedb/schemas/`. WAL mode on.
- FTS5 is available in the bundled driver: external-content `note_fts` (unicode61, diacritics folded) created with raw SQL in a `RoomDatabase.Callback.onCreate`, synced by insert/update/delete triggers. Verified: terms, prefix `layo*`, column filter `title:graph`, `bm25` ranking, update/delete sync. Query on 5k notes: <1 ms.
- Gotcha: Room checks `@Query` SQL at compile time and does not know FTS5 tables, so search uses `@RawQuery` with `RoomRawQuery` (wrapped by an extension function). Raw-SQL objects must also be created in migrations (Room's schema JSON does not describe them).
- Gotcha: bundled driver loads its native lib via `System.loadLibrary` → add `--enable-native-access=ALL-UNNAMED` to app JVM args (with spike 2).
- **Open:** macOS/Windows/x64 not testable here; PRD fallback is `@Fts4`. Migration test for a raw-SQL FTS table still to do in M1.

## 4. Mermaid subset (in-house, no ELK)
- **Decision: in-house.** ~350 lines pure Kotlin, zero new dependencies (ELK would add a multi-MB EPL dep and a Java-style API for what a small layered layout covers). Parser/layout are Compose-free and unit-tested; only `MermaidView` touches Compose (text measured via `TextMeasurer`, drawn on `Canvas`).
- Flowchart: `graph|flowchart TD/TB/LR/RL/BT`; shapes rect/round/stadium/circle/diamond; edges `-->`, `---`, `-.->`, `==>`, labels `|x|` and `-- x -->`; chains, `;`, `<br/>`. Layout: DFS cycle removal, longest-path layering, dummy nodes for long edges, 6 barycenter sweeps, neighbour-average x. Edges are polylines through dummy nodes (no splines, no port spreading, no edge-label collision avoidance).
- Sequence: participant/actor (+`as`), `->> -->> -> --> -x -)`, self messages, auto column widths.
- Unsupported (→ source shown with "Preview not available"): subgraph, style/classDef/click, loop/alt/opt/par/Note/box, other diagram types. Malformed input → error message + source.
- **Not done / open:** state diagrams (PRD MVP; can reuse flowchart layout, add `[*]` + `A --> B : ev` parser), Note over, activations, node shapes beyond five, big-graph timing, visual quality (human check).

## 5. JLaTeXMath
- `org.scilab.forge:jlatexmath:1.0.7` resolves from Maven Central, 670 KB, pure Java (AWT, headless-safe). `renderMath(latex, px, display)` → `BufferedImage` + baseline; `MathView` converts to a Compose `ImageBitmap`, rendered at density-scaled px so it stays sharp at 150%/200%.
- Inline vs display style verified (display is taller); fractions, sums, integrals, roots, matrices render. Warm render ~0.5 ms/formula (50-formula test), so no caching needed for notes; cache per (latex, size) in the real app anyway.
- Gotchas: unknown commands throw `ParseException` (good), but **unclosed `{` is silently accepted**, so `requireBalanced` pre-checks. Inline baseline alignment with surrounding text is exposed (`baseline`) but not wired into a text flow yet (demo uses Row centering). Colour is a render-time parameter (re-render on theme change).
- **Open:** inline math inside flowing Markdown text (needs an `InlineTextContent` placeholder using width/height/baseline), cold-start time, human visual check, GPL+Classpath notice in distribution.

## 7. Packaging (linux-arm64, local build)
- `packaging/linux/build.sh`: `./kotlin package` jar -> `jdeps` module list -> `jlink` (java.base, java.desktop, jdk.unsupported; 59 MB runtime) -> `jpackage --type app-image` -> `.deb` via `dpkg-deb` (jpackage deb needs fakeroot, absent) and tar.gz.
- Sizes (spike app, includes Compose+Skiko+JLaTeXMath+Room): app image 101 MB, .deb 73 MB, tar.gz 77 MB. Skiko native + Compose dominate; no PDFium bundled yet (+~6 MB).
- Gotcha: Amper `executable-jar` flattens libs by filename; androidx vs org.jetbrains `runtime-desktop` collide and the stub wins -> `NoClassDefFoundError: Composer`. Script picks the largest jar per name.
- Launch reaches Compose (headless box: HeadlessException only). Startup time / RSS not measured (no display here); human check.
- **Open:** AppImage needs `appimagetool` download (needs approval); x64 build needs an x64 machine (jlink/jpackage are per-arch, no cross); `.deb` Depends list untested on a clean system.
