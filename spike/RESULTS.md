# Milestone 0 results

## 1. Cross-block selection
- Design: per-block highlight drawn in each item (`drawBehind`) instead of one global overlay, because LazyColumn only composes visible items; text extraction uses the text-only `SelectionModel`, so off-screen blocks are included.
- Unit-tested: ordering, backward selection, word edges, paragraph, clamp. **Pending human check:** drag feel, auto-scroll, 100/150/200% scale (`-Dsun.java2d.uiScale=1.5`).

## 2. PDFium via FFM (linux-arm64)
- Pure Java 25 FFM binding (`spike/src/pdf/Pdfium.kt`, ~100 lines, no JNI/extra libs): open, page size, BGRA render, text extract, char boxes.
- Works against the arm64 `libpdfiumlo.so` shipped with LibreOffice (full FPDF/FPDFText API). Page 1 of a real spec PDF: 914x1183 render in ~7 ms (warm-ish, single run), text extraction correct.
- Needs `--enable-native-access=ALL-UNNAMED` to silence the warning (set in app JVM args).
- **Open:** for shipping we must bundle our own pdfium-binaries (linux-arm64 + x64); download needs approval. Cold-start and large-page timings not yet measured.
