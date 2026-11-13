# Milestone 0 results

## 1. Cross-block selection
- Design: per-block highlight drawn in each item (`drawBehind`) instead of one global overlay, because LazyColumn only composes visible items; text extraction uses the text-only `SelectionModel`, so off-screen blocks are included.
- Unit-tested: ordering, backward selection, word edges, paragraph, clamp. **Pending human check:** drag feel, auto-scroll, 100/150/200% scale (`-Dsun.java2d.uiScale=1.5`).
