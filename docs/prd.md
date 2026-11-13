# Product Requirements Document
## Personal Reading & Learning App — MVP

**Status:** Draft — revision 2.2 (2026-11-13; see §17 Revision notes)  
**Product type:** Open-source (Apache-2.0), local-first **Linux desktop** application (macOS/Windows later; no mobile), with an optional paid hosted AI service ("Athena AI")  
**MVP scope:** P0 + P1 (see §11). Acceptance criteria in §12 cover both.  
**Primary purpose:** Help users understand, retain, and connect what they read.  
**Working principle:** Read → Understand → Capture → Revisit

---

# 1. Product Vision

Build a personal reading application that makes it easier to understand difficult material, learn vocabulary while reading fiction, capture useful insights, and revisit knowledge later.

Unlike conventional ebook readers, the application should help users understand what they are reading without constantly interrupting the reading experience.

Unlike conventional knowledge-management applications, it should start with books and reading sessions rather than notes, tasks, or an abstract knowledge graph.

The application should support two reading experiences:

1. **Enjoyment reading:** Primarily novels and general nonfiction. Minimize interruptions and provide contextual vocabulary assistance.
2. **Learning reading:** Primarily technical books and educational material. Help readers understand mechanisms, identify prerequisites, capture insights, and revisit difficult concepts.

The application is a single-user, local-first app. It is free and open source (Apache-2.0). Everything works without an account: reading, notes, concepts, review, and AI through the user's own API key or a local model.

The only commercial component is **Athena AI**, an optional hosted AI provider. It requires signing in (Keycloak, `sso.ukata.io`) and a paid subscription with optional top-ups. It is specified separately in `docs/athena-ai-service.md`. The app never requires Athena AI, and nothing in the app is locked behind it except the convenience of AI with no setup.

# 2. Goals and Non-goals

## 2.1 Goals

The MVP must:

- Read PDF and EPUB documents.
- Preserve reading positions across sessions.
- Provide contextual AI explanations for selected passages.
- Provide contextual vocabulary assistance.
- Allow users to create and organize personal notes.
- Preserve references to the original book, chapter, page, and passage.
- Connect related concepts across different books.
- Support lightweight revision and reminders.
- Work offline for all non-AI reading and note-taking functionality.
- Keep user data locally stored and exportable.

## 2.2 Non-goals

The MVP will not attempt to become:

- A SaaS product or collaborative reading platform. (The optional Athena AI subscription is a paid AI provider, not a hosted copy of the app; books, notes, and reading data never leave the device except for the per-request AI context.)
- A replacement for Anki or a full spaced-repetition system.
- A general-purpose note-taking application.
- A complete Obsidian replacement.
- A sophisticated visual knowledge-graph editor.
- A research paper management system.
- An AI agent platform with multiple autonomous agents.
- A cloud-dependent application. No feature except Athena AI itself needs a server or an account.
- A full document editing application.

# 3. Target Users and Use Cases

The initial target user is a technically capable individual who reads a mixture of technical books, fiction, and nonfiction.

## 3.1 Technical reading

Typical scenario:

1. Open a technical PDF.
2. Read a section about a new concept.
3. Encounter a paragraph that is difficult to understand.
4. Select the paragraph and request an explanation.
5. Ask for a mechanism-level explanation or concrete example.
6. Save the explanation or write a personal interpretation.
7. Link important concepts to related concepts from other books.
8. Revisit the material later.

Primary pain points:

- Difficult writing styles and explanations.
- Repeatedly rereading paragraphs without understanding them.
- Losing the current line or paragraph while reading.
- Forgetting previously learned concepts.
- Difficulty connecting information across different books.

## 3.2 Novel and general reading

Typical scenario:

1. Open an EPUB novel.
2. Encounter an unfamiliar word or expression.
3. Tap the word to understand its meaning in context.
4. Continue reading without navigating away.
5. Occasionally save an interesting expression.
6. Revisit useful vocabulary later.

Primary pain points:

- Unfamiliar vocabulary interrupts reading.
- Dictionary definitions lack the context of the sentence.
- Idioms, slang, and phrasal verbs are difficult to understand.
- Looking up words repeatedly makes reading unpleasant.

# 4. Product Structure

The MVP has three main areas.

## 4.1 Library

Responsibilities:

- Import and manage PDF and EPUB files.
- Display books and reading progress.
- Resume the last reading position.
- Search the local library.
- Display basic book metadata.

Book metadata:

- Title
- Author
- Format
- Cover
- File location
- Reading progress
- Last opened timestamp
- Reading mode
- Optional user-defined tags

## 4.2 Reader

The reader is the primary application experience.

It should provide:

- Document rendering.
- Position persistence.
- Text selection.
- Contextual AI actions.
- Highlights and annotations.
- Notes.
- Reading-specific tools.

The reader has two modes:

**Novel mode**
- Vocabulary lookup.
- Sentence explanation.
- Contextual translation.
- Vocabulary history.
- Minimal interruption.

**Technical mode**
- Paragraph selection.
- AI explanation.
- Mechanism exploration.
- Example generation.
- Prerequisite identification.
- Understanding markers.
- Notes and concept linking.

The user can choose a mode when opening a book. The application may suggest a mode based on the document, but must not force an automatic classification.

The mode is stored per book and can be changed at any time from the reader. Mode controls which tools are shown first and the default AI behavior (e.g. spoiler protection, FR-023). It does not hide functionality: technical-mode actions remain available in novel mode through a secondary menu, and vice versa. Mode is independent of format: a PDF novel or a technical EPUB are both valid.

## 4.3 Review

Responsibilities:

- Display review items that are due (notes, explanations, passages, concepts, revision questions).
- Display passages and concepts marked as unclear.
- Reopen the original source passage.
- Review saved vocabulary.
- Record a review outcome for each item: understood, still unclear, or no longer relevant (see FR-041 for how outcomes affect scheduling).

Review should be lightweight and should not turn reading into a gamified productivity exercise. No streaks, scores, or daily quotas.

## 4.4 Secondary screens

These are reachable from the primary navigation (§6.4) and do not get top-level tabs:

- **Concepts:** concept list, search, and concept detail (FR-036). Reached from My Notes and from links inside notes.
- **Vocabulary:** vocabulary history with known/unknown filter (FR-022). Reached from My Notes and from the reader in novel mode.
- **Settings:** AI providers, privacy consent, preferred explanation language, review intervals, notifications, reading defaults, data directory, export/restore.

# 5. Functional Requirements

## 5.1 Document management

**FR-001 — Import documents**

The user can import PDF and EPUB files from the device.

**FR-002 — Local storage**

By default, importing **copies** the file into the application's library directory. This keeps the data directory self-contained and portable (§10). The user can optionally choose "link in place" for large collections. Linked files are stored by absolute path.

On import, the application computes a content hash (SHA-256) of the file. The hash is the identity used for:

- Detecting duplicate imports. The user is offered "open existing" instead of creating a second record.
- Relinking a moved or missing file. If a linked file is missing, the book stays in the library marked as *unavailable*. All its annotations, notes, and vocabulary remain accessible. The user can locate the file, and the application verifies the hash before relinking. A hash mismatch shows a warning but can be accepted (e.g. for a re-downloaded edition), after which locations are re-anchored on a best-effort basis (§7.0).

**FR-003 — Reading position**

The application automatically saves the last reading position.

For PDF:
- Document ID
- Page number
- Optional scroll offset or viewport position

For EPUB:
- Document ID
- EPUB CFI or equivalent stable reading location

Positions are saved with debouncing (e.g. at most every 2 seconds while navigating, plus on page change, book close, and application exit). Saving must never block rendering.

**FR-004 — Resume reading**

Reopening a book restores the last saved position.

**FR-005 — Metadata**

The application extracts available title, author, and cover metadata. Users can edit these fields.

**FR-006 — Reading progress**

Display approximate reading progress. Do not claim precise completion percentages where the document format cannot provide reliable information.

## 5.2 Reading experience

**FR-007 — Text selection**

Users can select words, sentences, paragraphs, or multiple paragraphs when supported by the document renderer.

**FR-008 — Preserve source context**

For every selection, retain:

- Selected text.
- Surrounding text where available.
- Book ID.
- Chapter or section.
- Page number or EPUB location.
- Selection coordinates or other source-location metadata where available.

All of this is captured in a single, format-aware `SourceLocation` value (§7.0), which every artifact that points into a book reuses: annotations, notes, AI threads, vocabulary, concept references, and review items.

**FR-009 — Reading ruler**

Provide an optional reading ruler for PDF reading.

The ruler may follow the cursor or be moved using keyboard controls.

It must be optional and should not interfere with text selection.

**FR-010 — Paragraph focus**

Provide an optional paragraph-focused reading experience where technically feasible.

This is an experiment rather than a mandatory reading behavior. The user should be able to disable it.

**FR-011 — Reading customization**

Provide basic adjustments:

- Font size where supported.
- Line spacing where supported.
- Reading width where supported.
- Light and dark themes.
- Reading background and contrast.

PDF layout must not be destructively modified.

## 5.3 Contextual AI assistance

This is one of the core differentiating features.

**FR-012 — Explain selection**

When text is selected, the user can request an explanation.

The AI receives the selected text and relevant surrounding context.

**Default context budget** (configurable in Settings):

- Book title, author, and the current chapter/section heading.
- The selection itself.
- The paragraph containing the selection, plus up to ~2 preceding paragraphs and 1 following paragraph. In novel mode, no following text is sent (FR-023).
- A hard cap of roughly 1,500 words of surrounding context per request. If the cap is hit, the context is trimmed away from the selection.

Responses stream into the explanation panel as they arrive. The user can cancel a request at any time.

**FR-013 — Explanation types**

Provide these predefined actions:

1. Explain simply.
2. Explain the underlying mechanism.
3. Give a concrete example.
4. Explain prerequisites.
5. Explain the author's reasoning.
6. Ask a custom question.

"Ask a custom question" starts a new thread on the current selection with a user-written prompt. FR-015 covers continuing an existing thread. Prompt templates for the predefined actions live in the `ai-client` module, are versioned, and their version is recorded with each AI message.

**FR-014 — Explanation quality**

The AI should not merely paraphrase the selected text.

It should attempt to:

- Identify the central claim.
- Explain unfamiliar terminology.
- Identify assumptions.
- Explain causal relationships.
- Reveal hidden prerequisites.
- Use concrete examples.
- Distinguish what the source explicitly states from additional interpretation.

If context is insufficient, the AI should say so rather than inventing missing information.

**FR-015 — Follow-up questions**

The user can ask follow-up questions about the explanation without selecting the passage again.

The application should preserve the relevant source context during the conversation.

An explanation and its follow-ups form an **AI thread** (§7.15). Every thread is stored locally, so its history survives restarts, but it is not shown in My Notes unless the user saves it. Unsaved threads can be browsed from the passage's annotation popover. They can be purged automatically after a configurable period (default: keep).

**FR-016 — Save explanation**

The user can save an AI explanation as an artifact associated with the original passage.

AI-generated content must be distinguishable from user-authored notes.

Saving creates a note with `origin = ai` (§7.5). The note is linked to its thread and to the source location. The user chooses to save either the whole thread or a single answer. The saved Markdown is a snapshot: it records the provider, the model, and the date. After saving, the user may edit it. Once edited, it is marked `origin = ai_edited`, so it remains distinguishable from purely user-authored notes.

**FR-017 — AI provider configuration**

Support a pluggable AI provider interface.

Initial support should include an OpenAI-compatible HTTP API, allowing compatible hosted services and local inference servers such as Ollama.

The application must not require a particular cloud provider.

A provider configuration consists of: display name, base URL, model name, an optional API key (stored in OS secure storage, §9), timeout, and a flag that classifies the provider as *local* or *external*. Endpoints on `localhost`/loopback are classified as local by default. Multiple providers can be configured, and one is the default. The application must expose a "test connection" action.

Provider types:

1. **OpenAI-compatible with API key** (bring your own key): any compatible hosted service.
2. **OpenAI-compatible, no auth**: local servers such as Ollama or LM Studio.
3. **Athena AI** (FR-045): built in, so the user doesn't enter a URL or key. It authenticates with the user's Athena account token instead of an API key.

All three use the same client and request pipeline (§8.5). Athena AI differs only in authentication and in how usage and plan errors are shown.

The AI is never required: with no provider configured, every non-AI feature works and the AI actions show a short "configure a provider" hint instead of an error.

**FR-018 — AI privacy**

Before sending content to an external provider, clearly indicate that the selected text and relevant context will be transmitted.

Only send the minimum necessary content.

No document should be uploaded in its entirety by default.

AI features must fail gracefully when offline or unavailable.

**Disclosure model.** This replaces per-request confirmation, to stay consistent with §6.1's "no excessive confirmation dialogs":

1. The first time an *external* provider is used, show a one-time consent dialog. It explains what will be sent: selection, surrounding context, book title and author, and chapter heading. Consent is recorded per provider.
2. Every AI panel shows a persistent indicator naming the provider and whether it is local or external.
3. An optional "preview request" toggle shows the exact payload before sending.
4. Local providers do not require the consent dialog.

On failure (offline, timeout, HTTP error), the selection and any typed question are kept, and the user can retry with one action.

## 5.4 Novel reading assistance

**FR-045 — Athena AI account (optional)**

The user can sign in to an Athena account to use the hosted Athena AI provider. Signing in is offered only from AI settings and from the "configure a provider" hint. It is never shown at startup, and never needed for anything else.

- **Protocol:** OpenID Connect against Keycloak (`https://sso.ukata.io`), using Authorization Code with PKCE (S256) in the system browser and a loopback redirect (`http://127.0.0.1:<ephemeral port>/callback`), following RFC 8252. The app is a public client and contains no client secret. If the realm cannot accept ephemeral loopback ports, fall back to the OAuth Device Authorization Grant.
- **Tokens:** the access token is held in memory only. The refresh token is stored in the OS keychain (§9). Refresh is silent. Sign-out revokes the refresh token and clears the keychain entry.
- **Data the app receives:** only the account's display name, email, plan, and remaining allowance. The app does not sync books, notes, or any library data to the account.
- **Offline:** signed-in state persists offline. AI requests fail gracefully as with any other provider (FR-018).

**FR-046 — Plan, usage, and purchase**

- AI settings show the current plan, the remaining monthly allowance, top-up credits, and the renewal date. All of this comes from the service's usage endpoint (`athena-ai-service.md`).
- "Upgrade" and "Buy top-up" open the billing pages in the system browser. There are no in-app payment forms. Payment is handled by the merchant of record.
- When the allowance is exhausted (HTTP 402) or rate-limited (HTTP 429), the AI panel shows a calm, specific message: what happened, when the allowance renews, and a top-up link. The selection and question are preserved (FR-018). A nag never appears unless the user has just hit a limit.
- A user can switch providers at any time. An exhausted allowance can be bridged with their own key or a local model, with no lock-in.

**FR-019 — Word lookup**

Tapping or selecting a word provides contextual meaning.

The explanation should consider the sentence and surrounding passage, rather than relying exclusively on a dictionary definition.

Interaction details:

- On desktop, **double-click** a word (or select a phrase) to open a compact lookup popover anchored near the word. Single click and arrow/space keys stay reserved for navigation, so lookup never conflicts with page turning.
- The popover appears within ~150 ms with the word and a loading state. Contextual meaning streams in after that.
- The compact answer comes first: contextual meaning plus translation in one or two lines. "More" expands to the full FR-020 explanation.
- Results are cached by (normalized expression, book, sentence). Re-opening the same lookup does not trigger a new AI call.
- Offline or with no provider: the popover shows the word, its saved meaning if it was looked up before, and lets the user save it to vocabulary for later. An offline dictionary fallback is a P2 item (§11).

**FR-020 — Vocabulary explanation**

Provide:

- Meaning in the current context.
- Preferred-language translation.
- Pronunciation where available.
- Example sentences.
- Idiom, slang, or phrasal-verb interpretation where relevant.

The preferred explanation language should be configurable.

**FR-021 — Sentence assistance**

Allow users to request:

- Sentence explanation.
- Sentence structure analysis.
- Expression explanation.
- Simpler paraphrase.
- Contextual translation.

**FR-022 — Vocabulary history**

Store words and expressions that the user has looked up.

Track:
- Word or expression.
- Context.
- Source book and location.
- Lookup timestamp.
- User-defined known/unknown status.

Every successful lookup is automatically recorded in vocabulary history. This is history, not review. Repeated lookups of the same expression (matched by normalized form: case-folded, trimmed, and lemmatized where available) increment `lookup_count` and add another context occurrence (§7.12) rather than creating duplicates. New entries default to status `unknown`. Vocabulary enters review only when the user explicitly adds it, or, optionally, when an `unknown` entry reaches a configurable lookup count (default off).

Do not automatically create a flashcard for every lookup.

**FR-023 — Spoiler protection**

When assisting with fiction, the AI should use only text available up to the current reading position.

It must not reveal future plot events or information from unread chapters.

Enforceable rules (active when the book is in novel mode; can be toggled per book):

1. **Hard rule (application):** no text after the end of the current selection, or after the current page/location, is ever included in the request context.
2. **Soft rule (prompt):** the system prompt instructs the model not to use outside knowledge of the work's plot, characters' fates, or later events, and to answer only from the provided text and general language knowledge.
3. The UI must not claim spoiler-safety is guaranteed. Well-known books may still leak through the model's training data. The novel-mode provider indicator shows "spoiler-guarded" rather than "spoiler-free".

## 5.5 Highlights and notes

**FR-024 — Highlight passages**

Users can highlight selected passages.

Highlights must retain their original source references.

A highlight has a color (from a small fixed palette) and an optional short comment. Longer commentary belongs in a linked note. Highlights are rendered as overlays: PDF files are never modified, and EPUB content files are never rewritten.

**FR-025 — Personal notes**

Users can create notes associated with:

- A book.
- A chapter.
- A page or EPUB location.
- A specific passage.
- A concept.

**FR-026 — Distinguish artifact types**

The application must distinguish:

1. Original source highlights.
2. User-authored notes.
3. AI-generated explanations.
4. Revision questions.

These artifacts must not overwrite one another.

Storage mapping (§7): highlights → `annotations`; user notes and saved AI explanations → `notes`, distinguished by `origin` (`user` / `ai` / `ai_edited`); revision questions → `review_items.question`. Each type has a distinct visual treatment wherever it appears, for example an "AI" badge and a different accent on AI-originated notes.

**FR-027 — Markdown editor**

Notes use Markdown as their canonical content format.

Support:

- Headings.
- Lists.
- Checklists.
- Tables.
- Code blocks.
- Basic mathematical notation.
- Links.

Details:

- Flavor: CommonMark + GFM (tables, task lists, strikethrough). Math uses `$…$` / `$$…$$`, rendered natively (JLaTeXMath on desktop, §8.2). Unsupported LaTeX falls back to showing the source in monospace.
- Editing model: a plain-text Markdown editor with live preview (split or toggle). No WYSIWYG editor (§13 risk: "note-taking becomes too complicated").
- Internal links use stable URIs that remain readable as plain text outside the app, for example `[TLB](reader://concept/<uuid>)` and `[p. 212](reader://book/<uuid>#loc=<encoded>)`. Exported Markdown rewrites them to relative file links or plain text (FR-031).
- Each note file starts with YAML front matter: `id`, `title`, `origin`, `book`, `source` (human-readable location), `concepts`, `created`, `updated`. That keeps the file self-describing outside the app.
- Saves are atomic (write to a temp file, then rename) and autosave on a short debounce.

**FR-028 — Mermaid diagrams**

Support Mermaid diagrams as an optional Markdown extension.

Initial supported diagram types should include:

- Flowcharts.
- Sequence diagrams.
- Class diagrams.
- State diagrams.
- Entity relationship diagrams.

Users must be able to edit the Mermaid source and preview the rendered diagram.

Mermaid source is stored inline in the note as a fenced ```` ```mermaid ```` block, so it stays readable and renderable outside the app (GitHub, Obsidian, VS Code). The app renders diagrams **natively**, with no web engine (§8.2): it parses the Mermaid syntax, lays out the diagram, and draws it in Compose. The native renderer supports a subset. In the MVP that is flowcharts, sequence diagrams, and state diagrams; class and ER diagrams are P2. Diagrams the renderer cannot handle (unsupported type or syntax) show their source with a "preview not available" label. The source is never altered. A render error shows the error message beside the source and never loses the source.

**FR-029 — AI-generated diagrams**

Allow users to request a Mermaid diagram based on an explanation or selected passage.

Generated diagrams are drafts. The user must review and save them explicitly.

**FR-030 — My Reading Notes**

Provide a searchable screen containing notes and saved explanations across all books.

Users can navigate from a note back to its original source passage.

Filters: book, origin (user/AI), concept, has-source, and date. Full-text search (SQLite FTS5) covers note bodies and titles, highlight text and comments, concept names, aliases, and descriptions, and vocabulary expressions and meanings. Search does **not** cover the full text of the books in the MVP. Library search (§4.1) covers book metadata and tags only.

If a note's source book is unavailable or deleted, the note still opens and shows the stored source text and location, with a "source unavailable" marker.

**FR-031 — Export**

Support exporting notes in ordinary Markdown files.

Also support exporting structured application data, including metadata, relationships, vocabulary, and review information, in a documented JSON format.

The user must not be locked into the application to access their notes.

Export details:

- **Markdown export:** one folder per book plus a `_general/` folder. Each note is a `.md` file with front matter (FR-027). Highlights for each book are exported as a single `highlights.md` file, with the quote, location, and comment for each highlight. Saved AI explanations are included and labeled as AI-generated. `reader://` links are rewritten to relative links where the target was exported, or to plain text otherwise.
- **JSON export:** a single archive (`.zip`) containing `manifest.json` (with `format_version`, app version, and export date), one JSON file per entity type, and the note Markdown files. Book files are excluded by default and included optionally. The schema is documented in the repository and versioned with semver.
- **Restore (FR-043):** the JSON archive can be imported back. Round-trip fidelity is tested (§13).

**FR-043 — Restore from export**

The user can restore a JSON export archive into an empty library, or merge it into an existing one. When merging, records are matched by UUID and the newer `updated_at` wins. Books are matched by content hash. If an export does not include the book files, the restored books appear as *unavailable* until the user relinks them (FR-002).

**FR-044 — Deletion**

The user can delete a book, note, highlight, concept, relationship, vocabulary entry, or review item.

- **Deleting a book** asks whether to also delete its highlights, notes, AI threads, and vocabulary. The default is to keep notes and vocabulary, detached but still showing their stored source text. Library-copied files are deleted. Linked files are never deleted.
- **Deleting a concept** removes its relationships and references, but not the notes that referenced it.
- Deletion is immediate, with an undo window of a few seconds. There is no trash folder in the MVP.

## 5.6 Concept relationships

The goal is to help users connect ideas across books.

**FR-032 — Concept records**

Users can create concepts with:

- Name.
- Description.
- Optional aliases.
- Created timestamp.
- Updated timestamp.

**FR-033 — Link concepts**

Users can link concepts with a named relationship.

Examples:

- Uses.
- Depends on.
- Implements.
- Enables.
- Is an example of.
- Contrasts with.
- Is related to.

Relationships should be directional where appropriate.

The application ships a predefined set of relationship types. Each type has a forward label, an inverse label, and a symmetric flag. For example: *uses / is used by*, *depends on / is a prerequisite of*, *implements / is implemented by*, *enables / is enabled by*, *is an example of / has example*, and the symmetric *contrasts with* and *is related to*. Users can add custom types. A concept detail screen shows each edge with the label read from that concept's side. Each (source, target, type) combination is unique. A symmetric relationship is stored once.

**FR-034 — Link notes to concepts**

A note, explanation, or source passage can reference one or more concepts.

**FR-035 — AI concept suggestions**

When saving an explanation, the application may suggest:

- Important concepts.
- Existing concepts that match.
- New concepts worth creating.
- Relationships between concepts.

The user must be able to accept, edit, or reject suggestions.

AI must not silently create authoritative relationships.

**FR-036 — Related concepts**

A concept detail screen should show:

- Its description.
- Related concepts.
- Relationship labels.
- Associated notes.
- Original book references.

**FR-037 — Search concepts**

Users can search concepts by name, alias, or description.

**MVP limitation:** No interactive knowledge graph visualization. No graph database. SQLite relational tables are sufficient.

## 5.7 Review and reminders

**FR-038 — Mark understanding**

Users can mark a passage or concept as:

- Understood.
- Unclear.
- Revisit later.

Marking a **passage** creates (or updates) an annotation of type `marker` at that location. In the PDF/EPUB view, a marker is shown as a subtle margin indicator, not a highlight. Marking a **concept** sets its `understanding` field. "Revisit later" also creates a review item on the first interval (FR-040). "Unclear" makes the item appear in Review's *unclear* list, without scheduling it automatically.

**FR-039 — Create revision question**

Users can create a question from a note or explanation.

Examples:

- Explain why a TLB is needed.
- What is the difference between virtual and physical addresses?
- Why does this particular mechanism work?

**FR-040 — Lightweight review schedule**

Provide configurable review intervals.

Suggested defaults:

- 1 day.
- 3 days.
- 7 days.
- 14 days.
- 30 days.

These are simple scheduling defaults, not a sophisticated spaced-repetition algorithm.

A review item keeps a step index into the interval list. Outcomes:

- **Understood:** advance one step. Past the last step, the item becomes `done`.
- **Still unclear:** go back to step 0, due in 1 day.
- **No longer relevant:** the item becomes `dismissed`.
- **Snooze:** the user can manually postpone an item to a chosen date.

Each outcome is appended to a review history (§7.14).

**FR-041 — Review actions**

A review item can offer:

- Recall the concept.
- Revisit the source passage.
- Reflect on a personal note.
- Reconsider an unclear explanation.

For a recall-type item, the question is shown first, and the linked note, explanation, or passage is revealed on request.

**FR-042 — Local notifications**

The application can send local notifications for scheduled reviews.

Notifications must be configurable and must not require a server.

On desktop, notifications are delivered only while the application is running. The application may optionally keep running in the system tray. Notifications are batched: at most one daily digest ("5 items due"), at a user-chosen time, never one notification per item. Items that came due while the application was closed appear in Review the next time it opens. Notifications can be turned off entirely.

Novel vocabulary should be reviewed lightly, without automatically turning every lookup into a quiz.

# 6. UX Requirements

## 6.1 Core interaction principle

Reading should remain the primary activity.

AI and note-taking features must be available when needed without permanently occupying the reading interface.

Avoid:

- Constant AI suggestions.
- Unsolicited popups.
- Gamification.
- Mandatory note-taking.
- Excessive confirmation dialogs.
- Automatic creation of large numbers of revision items.

## 6.2 Technical reading workflow

Expected workflow:

1. Open a PDF.
2. Select a difficult paragraph.
3. Choose Explain.
4. Read the explanation alongside the source.
5. Ask a follow-up question if necessary.
6. Save the explanation or write a personal note.
7. Optionally link the note to a concept.
8. Optionally schedule a review.

## 6.3 Novel reading workflow

Expected workflow:

1. Open an EPUB.
2. Tap an unfamiliar word.
3. View a compact contextual definition.
4. Optionally expand to sentence explanation.
5. Return to reading without losing position.
6. Revisit saved vocabulary later if desired.

## 6.4 Interface structure

Primary navigation:

- Library
- Review
- My Notes

The reader is opened from the Library and occupies the main application workspace.

Keep the initial interface minimal.

Desktop layout: the reader occupies the main area. AI threads, notes, and lookup details open in a collapsible right-hand side panel, so explanations sit next to the source (§6.2 step 4). The panel is closed by default. Settings and the secondary screens (§4.4) are reached from a menu or icon, not tabs.

Keyboard support is expected for core actions: page navigation, explain selection, highlight, new note, toggle side panel, and toggle the reading ruler. Shortcuts are listed in a help overlay.

## 6.5 Visual design

**Direction: "quiet paper."** The book is the focus, and the application's controls recede. The app should feel like a well-set printed page with tools close at hand, not like a dashboard or a chat app with a book attached.

**Themes.** Three reading themes, applied to both the app's controls and the rendered content:

| Theme | Background | Text | Use |
|---|---|---|---|
| Paper | warm off-white (≈ `#F7F3EA`) | warm near-black (≈ `#2B2722`) | default, daytime |
| Sepia | tan (≈ `#EFE3CC`) | brown-black (≈ `#3B2F22`) | long sessions, lower glare |
| Night | warm dark grey (≈ `#1E1C1A`) | warm light grey (≈ `#D9D3C7`) | low light; optional true-black variant |

Avoid pure `#FFFFFF` and `#000000` for reading surfaces. The app can follow the OS light/dark preference or stay pinned to one theme. The reader can override the content theme independently of the app's controls. PDFs are rendered as-is by default, with an optional "tint page" mode for Sepia/Night that never modifies the file.

**Typography.** All fonts are bundled (offline) and under the SIL Open Font License:

- Reading (EPUB, notes body): a text serif, defaulting to Literata. Alternatives: Source Serif 4, Charter.
- Interface: Inter (or IBM Plex Sans).
- Code: JetBrains Mono.
- Accessibility option: Atkinson Hyperlegible.

EPUB text measure defaults to about 66 characters (adjustable from 55 to 80), with line height 1.5–1.6. The type scale is modest: the interface mostly uses two or three sizes.

**Colour.**

- One restrained accent colour (ink blue on Paper/Sepia, softened on Night) for focus, selection, and primary actions.
- Five highlight colours (yellow, green, blue, pink, purple), muted and tuned per theme. On light themes they use multiply-style blending; on Night they use lower opacity. Text under a highlight must keep WCAG AA contrast in every theme.
- Understanding markers (FR-038) are small margin glyphs, not colour fills: ✓ understood, ? unclear, ↻ revisit later.

**AI visual language.** AI content is calm and clearly attributed:

- a lightly tinted surface;
- a small "AI" label;
- a chip naming the provider, marked as local or external (FR-018);
- "spoiler-guarded" shown when it applies (FR-023).

No sparkle or "magic" iconography, and no anthropomorphic avatar. Saved AI notes keep this treatment, so they never look like user-authored notes (FR-026).

**Component style.** Material 3 is the base, but with custom tokens: flat surfaces, minimal elevation, thin hairline dividers, 6–8 px corner radii, and an 8 pt spacing grid. No floating action buttons, splash screens, or decorative illustrations.

**Layout and chrome.**

- The reader has three zones: an optional left rail (table of contents, annotations list), the page, and a collapsible right panel (AI threads, notes, lookup details).
- Toolbars auto-hide while reading and reappear on mouse movement to the edge or on a key press.
- Focus mode (F11 / Esc) hides all chrome.
- Library: a "Continue reading" row, then a cover grid with thin progress bars, and a list view as an alternative.
- Review: one calm card at a time. No streaks, scores, or confetti.

**Interaction.**

- The selection toolbar is compact and horizontal: Highlight · Explain · Note · Look up · More. It appears on mouse-up near the selection and never covers the selected text.
- A command palette (Ctrl/Cmd+K) reaches all actions and screens.
- Motion is 150–200 ms fades and slides, and none at all when the OS reduce-motion setting is on.

**Design tokens.** Colours, type, spacing, and radii are defined once, as a Compose theme in `shared`. Every surface is rendered natively: EPUB text, PDF overlays, Markdown, math, and diagrams. So that single theme styles everything, and there are no CSS mirrors to keep in sync. EPUB publisher styles are mapped onto the theme (fonts and colours are overridable, §8.2).

**Accessibility.** WCAG AA contrast in all themes, including highlights and AI surfaces. UI scaling from 90% to 150%. Every action reachable by keyboard, with visible focus rings. Colour is never the only carrier of meaning (highlights can also carry a label). Screen-reader support in Compose Desktop is limited; test it, and document any gaps.

**Design deliverables before Milestone 1 UI work.**

- The token file.
- Wireframes for Library, Reader with side panel, selection toolbar, word-lookup popover, My Notes, and Review.
- A theme mock of the reader in Paper, Sepia, and Night.

# 7. Data Model

Use SQLite as the primary structured database.

Use Markdown files for note content.

Use original PDF and EPUB files as separate local documents.

**Source of truth.** Markdown files are canonical for note *content*. SQLite is canonical for everything else and holds a derived copy of note text for FTS. At startup and when a note file changes, the application compares the file's mtime and hash with the stored values and re-indexes any changes. Notes edited outside the app are therefore picked up. A note file that has gone missing is reported, never silently dropped.

**Conventions.**

- Primary keys are UUIDv7 strings. They are stable across export, restore, and merge (FR-043).
- Timestamps are ISO-8601 UTC strings.
- Enumerations are stored as text and validated in the domain layer.
- Every mutable table has `created_at` and `updated_at`.
- The schema is defined with Room entities and migrated with versioned Room migrations. Schema JSON is exported and committed, so migrations can be tested. The database runs in WAL mode with foreign keys enabled.

**Data directory layout.**

```
<data-dir>/
  library.db
  books/<book-id>.<ext>          # copied imports (FR-002)
  covers/<book-id>.jpg
  notes/<note-id>.md
  exports/
```

The data directory is configurable. Moving it (§10 Portability) must keep working. To make that possible, every path inside the data directory is stored relative to it. Only "link in place" books store absolute paths.

## 7.0 SourceLocation (value object)

`SourceLocation` is the shared format for pointing into a book. It is stored as a JSON column named `location` wherever a table below lists it. It combines a format-specific locator with a format-independent text anchor, so that locations can be re-anchored if the file changes or the renderer changes:

```json
{
  "format": "pdf | epub",
  "chapter": "3.2 Address Translation",     // human-readable, nullable
  "pdf":  { "page": 212, "rects": [[x, y, w, h], ...] },     // page-normalized 0..1 coords
  "epub": { "cfi": "epubcfi(/6/14!/4/2/1:0)", "href": "ch03.xhtml", "progression": 0.42 },
  "quote": { "exact": "selected text", "prefix": "~32 chars before", "suffix": "~32 chars after" }
}
```

- `quote` follows the W3C Web Annotation TextQuoteSelector model. It is the fallback anchor when the CFI or page rectangles no longer resolve.
- `rects` cover multi-line PDF selections. Coordinates are normalized to page size, so they hold at any zoom level.
- Positions without a selection (e.g. a reading position or a page-level note) omit `quote` and `rects`.
- "Surrounding context" for AI calls is *not* stored in `SourceLocation`. It is stored with the AI message that used it (§7.10).

## 7.1 Books

- id
- title
- author
- format (`pdf` | `epub`)
- storage (`copied` | `linked`)
- file_path (relative to the data directory for copied books; absolute for linked books)
- content_hash (SHA-256)
- file_size
- cover_path (nullable)
- reading_mode (`technical` | `novel`)
- spoiler_guard (boolean, default true when in novel mode)
- progress (0..1, approximate, nullable; FR-006)
- status (`available` | `unavailable`)
- imported_at
- last_opened_at
- created_at, updated_at

Metadata extracted from the file is overwritten by user edits (FR-005). Unique on `content_hash`.

## 7.2 Book tags

- book_id
- tag

Primary key: (book_id, tag).

## 7.3 Reading positions

- book_id (primary key; one current position per book)
- location (SourceLocation, without quote)
- updated_at

The earlier draft had both `books.last_position` and this table. This table alone is now the single source. Position history is not stored in the MVP.

## 7.4 Annotations

Highlights and understanding markers on a passage.

- id
- book_id
- type (`highlight` | `marker`)
- location (SourceLocation, with quote)
- color (highlights only)
- comment (short, nullable)
- understanding (`understood` | `unclear` | `revisit_later`; markers only)
- created_at, updated_at

## 7.5 Notes

User notes and saved AI explanations. The body lives in `notes/<id>.md`.

- id
- title
- origin (`user` | `ai` | `ai_edited`)
- content_path
- content_hash, content_mtime (for change detection)
- book_id (nullable)
- location (SourceLocation, nullable)
- annotation_id (nullable; the passage this note is attached to)
- ai_thread_id (nullable; set when origin is `ai` or `ai_edited`)
- created_at, updated_at

## 7.6 Concepts

- id
- name (unique, case-insensitive)
- description (Markdown, short)
- understanding (`understood` | `unclear` | `revisit_later`, nullable)
- created_at, updated_at

## 7.7 Concept aliases

- concept_id
- alias

Primary key: (concept_id, alias).

## 7.8 Relationship types

- id
- forward_label (e.g. "depends on")
- inverse_label (e.g. "is a prerequisite of")
- symmetric (boolean)
- builtin (boolean)

## 7.9 Concept relationships

- id
- source_concept_id
- target_concept_id
- relation_type_id
- note (nullable, short justification)
- origin (`user` | `ai_accepted`)
- created_at

Unique on (source, target, type). A symmetric relationship is stored once, with `source_concept_id < target_concept_id`.

## 7.10 Concept references

Links a concept to the material that discusses it.

- id
- concept_id
- source_type (`note` | `annotation` | `ai_thread` | `book`)
- source_id
- book_id (nullable, denormalized for queries)
- location (SourceLocation, nullable)
- created_at

## 7.11 Vocabulary

- id
- expression (as first seen)
- normalized (lookup key; unique together with `language`)
- language (source language, e.g. `en`)
- contextual_meaning (from the most recent lookup)
- translation
- translation_language
- pronunciation (nullable; IPA or similar)
- example (nullable)
- kind (`word` | `phrase` | `idiom` | `phrasal_verb` | `slang`, nullable)
- status (`unknown` | `known`)
- lookup_count
- first_looked_up_at, last_looked_up_at
- created_at, updated_at

## 7.12 Vocabulary occurrences

Each lookup context of an expression. Satisfies FR-022's "context" and "source book and location".

- id
- vocabulary_id
- book_id
- location (SourceLocation, with quote = the sentence)
- contextual_meaning (the meaning in this particular context)
- looked_up_at

## 7.13 Review items

- id
- source_type (`note` | `annotation` | `concept` | `vocabulary` | `ai_thread`)
- source_id
- question (nullable; set for revision questions, FR-039)
- interval_step (index into the configured intervals)
- due_at
- last_reviewed_at (nullable)
- status (`active` | `done` | `dismissed`)
- created_at, updated_at

## 7.14 Review events

- id
- review_item_id
- outcome (`understood` | `still_unclear` | `dismissed` | `snoozed`)
- reviewed_at

## 7.15 AI threads and messages

The earlier draft's single-row "AI interactions" table could not represent follow-up conversations (FR-015). It is replaced by two tables.

**ai_threads**

- id
- book_id (nullable)
- location (SourceLocation of the original selection)
- request_type (`explain_simple` | `mechanism` | `example` | `prerequisites` | `reasoning` | `custom` | `word` | `sentence` | `diagram` | `concepts`)
- created_at, updated_at

**ai_messages**

- id
- thread_id
- role (`user` | `assistant`)
- content (Markdown)
- context_sent (JSON: the exact surrounding text included; user messages only)
- provider_name, model (assistant messages only)
- prompt_template_version
- status (`complete` | `error` | `cancelled`)
- error (nullable)
- token_usage (nullable JSON)
- created_at

Secrets (API keys) are never stored in SQLite (§9).

## 7.16 Settings and provider configuration

- **settings**: key/value table holding preferences such as preferred explanation language, review intervals, notification time, reading defaults, and the context budget.
- **ai_providers**: id, name, base_url, model, kind (`local` | `external`), timeout_ms, is_default, consent_granted_at (nullable), created_at, updated_at. The API key lives in OS secure storage, keyed by provider id.

## 7.17 Full-text search

FTS5 virtual tables (external-content, kept in sync by triggers or by the repository layer) over:

- note title + body
- annotation quote + comment
- concept name + aliases + description
- vocabulary expression + meaning

# 8. Technical Architecture

## 8.1 Architecture principles

- Local-first.
- Offline-capable.
- Modular.
- Provider-independent AI integration.
- Separate document rendering from application domain logic.
- Exportable data.
- No mandatory backend.

## 8.2 Technology stack

**Target platform decision:** the MVP targets **Linux desktop only**: x86-64 and arm64, on GNOME and KDE, under Wayland (through XWayland) and X11. Mobile (Android/iOS) is out of scope. macOS and Windows are post-MVP. The JVM keeps them cheap to add later, so platform-specific services (secrets, notifications, file dialogs) still sit behind interfaces, but the MVP implements, tests, and packages Linux only.

Linux-specific notes:

- Compose Desktop (AWT) runs on Wayland through XWayland. Test HiDPI and fractional scaling on GNOME and KDE early. Native-Wayland AWT (JDK Project Wakefield) is not relied on.
- Follow freedesktop conventions:
  - XDG base directories: data in `$XDG_DATA_HOME/athena`, config in `$XDG_CONFIG_HOME/athena`, cache in `$XDG_CACHE_HOME/athena`.
  - A `.desktop` entry and icon, and MIME associations for `application/pdf` and `application/epub+zip` ("Open with Athena").
  - The system light/dark preference read from the XDG Desktop Portal (`org.freedesktop.appearance color-scheme`).
  - File dialogs through the portal (FileChooser) where available, so they look native and work in a Flatpak sandbox.

**No embedded browser engine.** The app does not bundle Chromium (no Electron, CEF/JCEF, or KCEF), for footprint reasons (§10 resource targets). All content is rendered natively in Compose. Android is a possible later target, so domain, database, and AI modules are written as Kotlin Multiplatform `commonMain` code with no JVM-desktop or Android APIs in them.

| Component | Choice for desktop MVP | Notes |
|---|---|---|
| Build tool | Kotlin Toolchain (Amper) via the `./kotlin` wrapper | No Gradle. Gradle-plugin-only libraries (e.g. SQLDelight, the Compose Desktop packaging plugin) are unavailable; KSP processors are supported |
| Application | Kotlin/JVM with Compose Multiplatform Desktop. `shared` is a single-platform (`jvm`) library; `desktopApp` is the executable | No Android or other KMP targets. The module split in §8.3 is kept for structure and testability, not for multiplatform reuse |
| UI | Compose Multiplatform (Desktop) | |
| Embedded web engine | **None** | Deliberate. See the note above and OD-1's fallback |
| EPUB rendering | **Native Compose renderer** (`document-epub`): EPUB container/OPF/NCX/nav parsed in `shared`; XHTML parsed with jsoup; a supported CSS subset mapped to Compose text styles; layout with `TextMeasurer`; scroll and paginated modes; custom selection over `TextLayoutResult`; CFIs generated and resolved against the parsed DOM | Scope: reflowable EPUB 2/3. CSS subset: font style/weight/size, text-align, indent, margins, lists, blockquote, images, simple tables, footnote links. Fixed-layout EPUB, scripted content, and complex CSS layouts are out of MVP scope and fall back to a simplified flow. Readium Kotlin is Android-only and not used |
| PDF rendering | **PDFium** (prebuilt binaries for linux-x64 and linux-arm64) called through the Java Foreign Function & Memory API (final since JDK 22; no JNI glue). Pages are rasterized into Compose `ImageBitmap` tiles; a text layer is built from PDFium char boxes for selection and highlights | PDFium is BSD/Apache licensed and about 5–7 MB per platform. Apache PDFBox is the fallback if PDFium binding proves impractical. Avoid MuPDF (AGPL) |
| Structured database | SQLite via Room KMP (KSP code generation) with the bundled SQLite driver (`androidx.sqlite:sqlite-bundled`) | Room's annotations only model FTS3/4. FTS5 tables are created in migrations with raw SQL and queried through `@RawQuery` or the driver connection inside the repository layer. Milestone 0 verifies that the bundled driver has FTS5 enabled on linux-arm64/x64, macOS, and Windows; if not, use FTS4 via `@Fts4` |
| Note content | Markdown files (CommonMark + GFM) | Parser: JetBrains `markdown` or commonmark-java. Rendering: native Compose renderer (e.g. `multiplatform-markdown-renderer`, or a custom renderer over the parser AST) |
| Search | SQLite FTS5 | |
| Diagram rendering | **Native Mermaid-subset renderer** (`diagrams` module): a parser for flowchart, sequence, and state syntax; layered graph layout (Eclipse Layout Kernel `elk-core` + layered algorithm, or a small in-house Sugiyama implementation, chosen in the spike); sequence diagrams use a simple column layout; drawn on a Compose `Canvas` | ELK is EPL-2.0 (weak copyleft, file-level), which is compatible with distribution inside an Apache-2.0 app. Mermaid's JS is not used |
| Math rendering | **JLaTeXMath** (pure Java) renders to an image embedded in Compose | GPL-2.0 *with Classpath Exception*: linking from an Apache-2.0 app is permitted; ship its source/notice. |
| AI integration | Pluggable OpenAI-compatible client (Ktor), streaming via SSE | |
| Local notifications | freedesktop Desktop Notifications over D-Bus (`org.freedesktop.Notifications`, e.g. via dbus-java), plus an optional StatusNotifierItem tray icon | Only while the app is running (FR-042). GNOME has no tray icon without an extension, so the tray is optional and notifications must work without it |
| Secrets | `java-keyring` (Linux: Secret Service over D-Bus, i.e. GNOME Keyring or KWallet; it also covers macOS Keychain and Windows Credential Manager for later ports) | If no Secret Service is available (minimal window managers), warn the user and offer a session-only key; never plaintext in the DB or config files |
| Packaging | `./kotlin package` executable JAR, wrapped by `jpackage` (from a JDK 17+) into a bundled-runtime app image, shipped as **AppImage** and **.deb** for the MVP. A Flatpak (Flathub) is a post-MVP goal and needs the portal-based file access above | The Kotlin Toolchain 0.13 only packages `jar`/`executable-jar`/`aab`; there are no native installers. The `jpackage` step is a small script, run in CI for x64 and arm64. Use `jlink` to ship a trimmed runtime (only the needed JDK modules). PDFium native libraries are bundled per OS/arch |
| OCR fallback (P2) | Tesseract | Deferred |

**Milestone 0 technical spike (time-boxed, before feature work).** Build throwaway prototypes that prove the native stack on the test corpus (OD-1):

- PDFium via FFM renders pages and returns char boxes on linux-arm64 and linux-x64.
- The window, popovers, and selection behave correctly under Wayland (XWayland) and X11, at 100%, 150%, and 200% scale.
- The native EPUB renderer lays out a real novel and a technical EPUB with acceptable fidelity, in scroll and paginated modes.
- Word- and paragraph-level selection across blocks and pages is reliable in Compose for both formats.
- Highlight overlays render, and CFIs round-trip.
- A Mermaid flowchart and a sequence diagram render natively.
- JLaTeXMath renders inline and display math.
- The §10 resource targets hold.

Record the results. If a component fails, apply the OD-1 fallback.

The domain model must not depend on rendering or OS-specific APIs, so other desktop OSes can be added later without touching it.

## 8.3 Application modules

Suggested logical modules:

1. `core-domain`
   - Books, annotations, notes, concepts, relationships, review items.

2. `core-database`
   - SQLite schema and repositories.

3. `document-pdf`
   - PDF rendering, text selection, location mapping.

4. `document-epub`
   - EPUB rendering, CFI, text selection.

5. `reader`
   - Shared reading experience and interaction orchestration.

6. `ai-client`
   - Provider abstraction, prompt construction, request handling.

7. `knowledge`
   - Concepts, relationships, and source references.

8. `notes`
   - Markdown storage, editor, Mermaid preview, export.

9. `review`
   - Review scheduling and local notifications.

10. `render-markdown`
    - Native Markdown rendering (GFM), math rendering (JLaTeXMath behind an interface), and the editor's live preview.

13. `diagrams`
    - Mermaid-subset parser, layout, and Compose drawing (FR-028).

14. `account`
    - Athena AI sign-in (OIDC + PKCE), token storage, plan and usage display (FR-045, FR-046). Used only by the Athena AI provider in `ai-client`.

11. `data-portability`
    - Markdown export, JSON export/restore (FR-031, FR-043), and the documented export schema.

12. `vocabulary`
    - Lookup orchestration, normalization, the lookup cache, and history (FR-019–FR-022).

Dependency rule: `core-domain` depends on nothing. Feature modules depend on `core-domain` and interfaces only. `document-*`, `render-markdown`, `diagrams`, and platform services (secure storage, notifications) are injected implementations.

### 8.3.1 Portability rules

The MVP ships on Linux only (§8.2), but macOS is the expected first follow-up, with Windows after it. These rules keep that port cheap. They apply from the first commit, and code review enforces them.

1. **OS services sit behind interfaces** defined in `shared`, with Linux implementations in a platform package (e.g. `io.ukata.athena.platform.linux`). The interfaces cover: secrets, notifications, file and folder dialogs, system appearance (light/dark), application directories, file associations or "open with", and opening URLs in the browser. Feature code only sees the interfaces.
2. **No OS-specific APIs outside platform implementations.** No D-Bus, XDG, `/proc`, shell commands, or `xdg-open` calls in feature or domain code.
3. **Paths:**
   - Always use `java.nio.file.Path` APIs. Never concatenate strings with `/`, never hard-code `~` or `/home`.
   - Application directories come from the directory service (XDG on Linux).
   - Paths stored in the DB or exports are relative to the data directory and use `/` separators (§7).
4. **Keyboard shortcuts** are defined once, using a platform "primary modifier" (Ctrl on Linux and Windows, Cmd on macOS), never hard-coded Ctrl. Shortcut labels in the UI come from the same definition.
5. **Native libraries** (PDFium, the SQLite driver) are loaded through one loader that resolves the library by OS and architecture. Do not add native dependencies that lack macOS and Windows builds without recording a decision.
6. **Fonts are bundled** (§6.5), so layout and pagination are the same on every OS. Never depend on system fonts for reading content.
7. **Line endings and encoding:** read and write text as UTF-8 explicitly. Write note files with `\n`, and accept `\r\n` when reading.
8. **Process and window behaviour** (tray, single-instance, quit vs. close-to-tray) lives in `desktopApp` behind small abstractions, not in feature modules.
9. **Packaging scripts** are per-OS files under `packaging/<os>/`. Build logic must not assume Linux tools except inside `packaging/linux/`.
10. **Dependencies:** prefer pure-JVM libraries, or ones that support all three OSes (e.g. `java-keyring` over raw D-Bus bindings). Choosing a Linux-only dependency needs a note in §16.

## 8.4 Document abstraction

PDF and EPUB should use different rendering implementations.

Do not force both formats into an artificial shared rendering engine.

Instead, expose a common domain-level interface for:

- Current reading location.
- Text selection.
- Source metadata.
- Navigation.
- Position persistence.

Each renderer can provide format-specific location and selection information.

Each renderer must also be able to:

- Emit selections as a `SourceLocation` with `quote` filled in (§7.0).
- Resolve a `SourceLocation` back into the document for navigation and highlight overlays. It tries the format-specific locator first, then falls back to the quote anchor, and reports when a location cannot be resolved.
- Return text *before* a given location, up to a word budget, for AI context and spoiler-guarded requests (FR-012, FR-023).
- Report approximate progress (FR-006).

Text extraction for AI context must work without rendering the page on screen.

## 8.5 AI abstraction

The application should expose a common AI service interface.

Example operations:

- `explainPassage`
- `explainWord`
- `explainSentence`
- `answerFollowUp`
- `extractConcepts`
- `suggestRelationships`
- `generateMermaidDiagram`

Provider-specific implementation details should remain outside the domain layer.

AI requests should support timeouts, cancellation, retries where safe, and clear error messages.

Additional requirements:

- Streaming responses are supported and used by default when the provider supports them. Non-streaming providers are also supported.
- Operations that feed structured data back into the app (`extractConcepts`, `suggestRelationships`, `generateMermaidDiagram`) request JSON or fenced output and validate it. Invalid output is shown as plain text and never applied.
- **Prompt-injection hygiene:** book text is untrusted input. It is passed in clearly delimited context blocks, and the model's output never triggers actions automatically. Every suggestion still needs user acceptance (FR-035).
- The AI service is the only component that knows about providers. All requests pass through a single context builder, which enforces the context budget, spoiler rules, and disclosure checks (FR-012, FR-018, FR-023).

## 8.6 Concept relationships in SQLite

Use relational tables rather than a graph database.

A relationship is simply an edge between two concepts.

SQLite can support:

- Direct relationship queries.
- Reverse relationship queries.
- Multi-hop traversal using recursive CTEs.
- Search and filtering.

An interactive graph renderer can be added later without changing the fundamental storage model.

# 9. Security and Privacy

Because books, personal notes, and reading history may contain private information, privacy must be considered from the beginning.

Requirements:

- No account registration for the app. An account exists only for the optional Athena AI service (FR-045), and nothing else depends on it.
- No telemetry by default.
- No document upload by default.
- AI requests transmit only selected text and necessary context.
- Clearly disclose when data is sent to an external AI provider.
- Store API credentials in platform-secure storage.
- Avoid writing document contents or API secrets into application logs.
- Support deleting local books, notes, annotations, and related data.
- Provide a complete export mechanism.

**Athena AI privacy** (details in `athena-ai-service.md`):

- Athena AI is an *external* provider. The one-time consent and the provider indicator apply (FR-018).
- The service processes request content only to produce the response. It does not log or store prompts or responses. Upstream model providers are used under zero-data-retention terms where available. Usage records keep only user ID, timestamp, model tier, and token counts.
- Usage metering on the server is billing data, not app telemetry. The app itself still sends no telemetry.
- A public privacy policy and terms of service are required before launch.

The application should not claim that local storage is encrypted unless the implementation actually provides encryption. Device-level security and optional database encryption can be evaluated separately.

# 10. Non-functional Requirements

**Performance**

- Opening a previously imported document should feel immediate for normal-sized documents.
- Reading position updates should not interrupt rendering.
- Selecting text should feel responsive.
- Database operations should not block the UI.
- AI response latency should not block normal reading.

Indicative targets, used to judge "immediate" and "responsive", measured on a mid-range laptop:

| Operation | Target |
|---|---|
| Reopen a previously imported book (≤ 50 MB PDF / ≤ 5 MB EPUB) to the last position | < 1.5 s |
| Page turn / scroll to the next page | < 100 ms perceived |
| Selection → action menu visible | < 100 ms |
| Word lookup popover visible (before the AI answer) | < 150 ms |
| Local search across ~5,000 notes/highlights | < 200 ms |
| Library with 1,000 books stays usable | grid/list scrolls smoothly |

**Resource targets** (the reason for the no-Chromium decision; checked in Milestone 0 and in CI on release builds):

| Measure | Target |
|---|---|
| Installer / app image size (incl. trimmed JVM runtime and PDFium) | ≤ 120 MB per platform |
| Resident memory, idle at Library | ≤ 250 MB |
| Resident memory, reading a 50 MB PDF or a large EPUB | ≤ 400 MB |
| CPU while idle on a page | ~0% (no polling, no animation loops) |
| Cold start to Library | < 2 s |

AI latency is the provider's, so there is no target for it. The UI must show progress and support cancellation.

**Reliability**

- Reading position must survive application restarts.
- Notes should be saved safely.
- AI failures must not cause loss of the original selection or note.
- Missing source files must be reported clearly.
- Interrupted writes should not corrupt the user's library.

Mechanisms: SQLite WAL with transactions for multi-row changes, atomic temp-file-then-rename writes for note files, and an automatic local backup of `library.db` (rolling, e.g. the last 7 daily copies) inside the data directory.

**Offline operation**

The following must work without internet:

- Open local documents.
- Read and navigate.
- Save positions.
- Create and edit notes.
- Highlight text.
- Browse concepts.
- Search local notes and concepts.
- Review existing items.

AI services may be unavailable offline unless the user configures a local inference provider.

**Portability**

- Notes must be readable outside the application.
- Structured data must be exportable.
- Document references should be recoverable when moving the application data directory.

# 11. MVP Priorities

## P0 — Essential

- PDF and EPUB import and reading.
- Position persistence.
- Library management.
- Text selection and source references.
- Contextual AI explanation.
- Follow-up questions.
- Save AI explanations.
- Personal Markdown notes.
- Highlights and annotations.
- SQLite data model.
- Local-first operation.
- Export notes and structured data.
- Restore from JSON export (FR-043).
- AI provider configuration and external-transmission disclosure.
- Missing/moved file handling (FR-002).
- Deletion (FR-044).

## P1 — Core learning experience

- Novel reading mode.
- Contextual vocabulary lookup.
- Sentence explanation.
- Vocabulary history.
- Concept records.
- Manual concept relationships.
- Link notes and explanations to concepts.
- Search concepts and notes.
- Understanding markers.
- Lightweight review scheduling.
- Local notifications.
- Mermaid editing and native preview (flowchart, sequence, state).
- Math rendering in notes (native, JLaTeXMath).
- Spoiler guard (FR-023).

## P2 — Improvements

- AI-assisted concept extraction.
- AI-suggested relationships.
- AI-generated Mermaid diagrams.
- Reading ruler.
- Paragraph focus.
- More sophisticated review behavior.
- OCR fallback for scanned PDFs.
- TTS.
- Advanced reading statistics.
- Offline dictionary fallback for word lookup.
- Full-text search inside book contents.
- Trash / soft-delete with recovery.
- Native rendering of Mermaid class and ER diagrams.
- Fixed-layout EPUB support.

## Explicitly deferred

- Cloud synchronization.
- Dedicated graph database.
- Interactive knowledge graph visualization.
- Multi-agent orchestration.
- Collaborative notes.
- Social features.
- Automatic mind-map generation.
- Freehand diagram editor.
- Full spaced-repetition engine.
- Complex document editing.

# 12. MVP Acceptance Criteria

The MVP is considered successful when the following end-to-end workflows work reliably. Workflows A and F are P0 gates. Workflows B–E exercise P1 features.

Run every workflow on Linux, x64 and arm64, under both a GNOME Wayland session and a KDE (or X11) session, with at least one external provider and one local provider (e.g. Ollama).

## Workflow A — Understand a technical passage

- Import a technical PDF.
- Open and navigate it.
- Select a passage.
- Request a mechanism-level explanation. On first use of an external provider, the consent dialog appears once.
- Ask a follow-up question.
- Save the explanation.
- Close and reopen the application.
- Recover the saved explanation and original source reference. Clicking the reference reopens the PDF at the passage with the passage highlighted.
- Disconnect the network and repeat the request. A clear error appears, the selection is kept, and retry works once the network is back.

## Workflow B — Learn vocabulary while reading

- Import an EPUB.
- Open it in novel mode.
- Select an unfamiliar word.
- Receive a contextual explanation. Verify that the request payload contains no text past the current location.
- Return to the exact reading position.
- Find the word in vocabulary history.
- Mark it as known or unknown.

## Workflow C — Build connected knowledge

- Create a concept manually.
- Create another concept.
- Link them with a named relationship.
- Associate a note with one concept.
- Navigate to the related concept.
- Find the original source passage from the concept detail screen.

## Workflow D — Revisit previous learning

- Mark a concept or note for review.
- Schedule a review.
- Receive a local notification.
- Open the review item.
- Recall the concept or revisit the original passage.
- Mark it understood or schedule another review.

## Workflow E — Preserve personal knowledge

- Create a Markdown note.
- Add a Mermaid diagram.
- Save and reopen it.
- Export the note.
- Open the exported Markdown outside the application.
- Confirm that the content remains understandable.

## Workflow F — Survive change

- Highlight a passage and write a note in a book that was imported with "link in place".
- Move the book file elsewhere. The book shows as unavailable, and its notes remain readable.
- Relink the file. The highlight renders at the same passage again.
- Export the JSON archive, restore it into a fresh data directory, and confirm that books, notes, highlights, concepts, vocabulary, and review items are identical (same IDs and content).
- Kill the application during note autosave. On restart, the note holds either the old or the new content, never a corrupted mix.

# 13. Key Risks and Mitigations

| Risk | Mitigation |
|---|---|
| PDF text selection is unreliable | Test complex PDFs early; retain page coordinates and support OCR fallback |
| AI explanations are inaccurate | Preserve source text, distinguish interpretation from source claims, allow user correction |
| AI requests expose private content | Explicit external-transmission disclosure and minimal context |
| Concept extraction creates noise | Make suggestions optional and require user approval |
| Note-taking becomes too complicated | Keep Markdown canonical and avoid building a full editor |
| Too many features delay delivery | Enforce P0/P1 boundaries |
| PDF and EPUB behave differently | Keep separate rendering engines behind a shared domain interface |
| Export loses important metadata | Version the JSON export format and test round-trip restoration |
| Native EPUB renderer underestimates CSS/layout complexity | Strict supported-CSS subset; simplified fallback flow; test corpus in Milestone 0; fixed-layout EPUB out of scope |
| Cross-block text selection in Compose is hard (no built-in multi-paragraph selection with offsets) | Custom selection layer over `TextLayoutResult`s, proven first in Milestone 0; it is the highest-risk item |
| Native Mermaid renderer covers only part of Mermaid | Mermaid source stays canonical and portable; unsupported diagrams show their source; ship flowchart/sequence/state first |
| JVM baseline memory makes the app feel heavy | Resource targets (§10), trimmed runtime, tuned heap flags (e.g. `-XX:+UseSerialGC` or ZGC with a small max heap), tile-based PDF rendering with a bounded bitmap cache |
| Hosted AI costs exceed subscription revenue | Hard per-user allowance, model routing by request type, top-ups for heavy users, per-request context cap (`athena-ai-service.md`) |
| Upstream provider keys leak through the open-source client | Upstream keys live only on the server; the client authenticates with short-lived user tokens |
| Stored locations break when renderers or files change | `SourceLocation` stores a text-quote anchor alongside CFI/page rects; re-anchor on failure (§7.0) |
| Spoilers leak through the model's prior knowledge of famous books | Hard context cutoff plus prompt instruction; UI says "spoiler-guarded", not "spoiler-free" (FR-023) |
| Book text contains prompt-injection content | Treat book text as untrusted, delimit it, never auto-apply AI output (§8.5) |
| AI cost/latency makes word lookup unpleasant | Short compact-answer prompt first, cache results, support fast local models |
| Notes edited outside the app drift from the DB index | mtime/hash rescan on startup and on file change (§7) |

# 14. Product Success Metrics

This is a personal application, so conventional SaaS metrics such as retention, conversion, and monthly active users are not relevant.

Instead, evaluate:

- Can I resume reading without losing my place?
- Can I understand a difficult passage without leaving the reader?
- Can I find a previous explanation months later?
- Can I connect a concept learned in one book to another?
- Can I revisit something I previously struggled with?
- Can I export and preserve my accumulated knowledge?
- Does the application make reading more enjoyable rather than more demanding?

The most important qualitative measure:

**Does this application help me understand and retain more of what I read, without making reading feel like work?**

# 15. Implementation Strategy

Build incrementally.

Milestones are ordered so that every P0 item is in place by the end of Milestone 3, and every P1 item by the end of Milestone 5. P2 items come after the MVP.

**Milestone 0 — Technical spike (time-boxed)**
- Native stack prototypes: PDFium via FFM, native EPUB renderer, cross-block selection, Mermaid-subset render, JLaTeXMath (§8.2).
- Packaging smoke test: AppImage and .deb for x64 and arm64 (executable JAR + `jlink` + `jpackage`, PDFium natives included), measured against the §10 resource targets.
- Room on the Kotlin Toolchain: KSP setup, schema export, and an FTS5 table on the bundled driver.
- Confirm PDFium binaries and FFM binding work on linux-arm64 (the development machine).
- Exit: renderer choices recorded; OD-1 closed.

**Milestone 1 — Reading foundation**
- Data directory, SQLite schema v1, migrations.
- Library: import (copy/link, hash, duplicate detection), metadata editing, tags, library search.
- PDF reader and EPUB reader behind the document abstraction.
- `SourceLocation` capture and resolution.
- Position persistence and resume.
- Highlights (with comments).
- Missing/moved file handling.
- Reading customization (FR-011).

**Milestone 2 — AI-assisted understanding**
- AI provider settings, secure key storage, test connection.
- Disclosure and consent model.
- Context builder (context budget, spoiler cutoff).
- Explanation actions with streaming and cancellation.
- AI threads and follow-up questions.
- Save explanations as AI-origin notes.

**Milestone 3 — Personal knowledge and portability**
- Markdown notes (front matter, atomic save, native live preview, math, Mermaid-subset preview).
- My Notes screen with FTS search and filters.
- Navigation from a note back to its source.
- Markdown export, JSON export, and restore.
- Deletion with undo.
- ✅ End of P0. Workflows A and F pass.

**Milestone 4 — Concepts and novel reading**
- Concept records, aliases, relationship types, relationships, references, concept detail, and concept search.
- Novel mode: word lookup popover, vocabulary explanation, sentence assistance, spoiler guard.
- Vocabulary history and occurrences, known/unknown status.
- Workflows B, C, and E pass.

**Milestone 5 — Revisit**
- Understanding markers.
- Review items, revision questions, interval scheduling, review history.
- Review screen.
- Tray and daily-digest notifications.
- ✅ End of MVP. Workflow D passes.

**Milestone 6 — Athena AI (public release)**
- Account sign-in (FR-045), Athena AI provider, plan/usage display and limit handling (FR-046).
- Server built per `athena-ai-service.md` (separate, closed-source repository).
- Privacy policy, terms, and billing (merchant of record) live.
- Signed AppImage and .deb releases (x64, arm64), optionally an apt repository, plus an update check that only fetches a public version manifest and sends no identifiers.

**After MVP (P2)**
- AI concept and relationship suggestions, AI-generated Mermaid diagrams, reading ruler, paragraph focus, OCR, offline dictionary, and the other items listed in §11 P2.

At every milestone, the application should remain usable.

# 16. Open Decisions

| ID | Decision | Current leaning | Decide by |
|---|---|---|---|
| OD-1 | Native rendering stack: confirm PDFium-via-FFM, the native EPUB renderer design (pagination approach, CSS subset, selection layer), ELK vs. in-house layered layout for diagrams. **Fallback if the native EPUB renderer fails the spike:** the system WebKitGTK, which is not bundled and adds no installer size. It still costs memory, and embedding it in an AWT window is awkward, so it is only used with explicit sign-off. Bundled Chromium remains excluded. | Native Compose for everything; no browser engine | End of Milestone 0 |
| OD-2 | Default AI provider and model recommendations to document (cloud and local) | OpenAI-compatible; document Ollama setup | Milestone 2 |
| OD-3 | Lemmatization for vocabulary normalization: rule-based per language vs. asking the AI to return a lemma | AI returns a lemma; fallback is a lowercase match | Milestone 4 |
| OD-4 | Languages supported for "preferred explanation language" and book source language at launch | English books; any target language the model supports | Milestone 4 |
| OD-5 | Optional encryption of the database at rest (SQLCipher) | Not in MVP; rely on OS disk encryption (§9) | Post-MVP |
| OD-6 | ~~Licensing/distribution~~ **Decided:** the app is open source under Apache-2.0. Revenue comes from the optional Athena AI subscription (monthly plan with an allowance, plus top-ups), served by a closed-source service. Trademark the product name and logo, and do a name clearance search ("Athena" is crowded). Third-party dependencies must be Apache-2.0-compatible (no AGPL, e.g. MuPDF). | Closed | — |
| OD-7 | Athena AI pricing (plan price, allowance size, top-up packs), merchant of record (Paddle vs. Lemon Squeezy), and launch regions | See `athena-ai-service.md` §9 | Before Milestone 6 |

# 17. Revision Notes

Revision 2.2 (2026-11-13):

- **Portability rules (§8.3.1):** Linux-only MVP, but code stays portable for a later macOS port, then Windows. Secrets now use `java-keyring`.

- **Linux-only MVP:** dropped Android (and the Android build target) and deferred macOS/Windows. Linux specifics added to §8.2: Wayland/X11, XDG directories and portals, D-Bus Secret Service and notifications, AppImage/.deb packaging. Acceptance runs on GNOME and KDE, x64 and arm64.

- **Business model (OD-6 closed):** Apache-2.0 open-source app; optional paid Athena AI provider with Keycloak sign-in (FR-045, FR-046), specified in `docs/athena-ai-service.md`; Milestone 6 added; §1, §2.2, and §9 updated so accounts exist only for Athena AI.
- **No bundled browser engine:** removed JCEF/KCEF. EPUB, Markdown, math, and Mermaid are now rendered natively in Compose (§8.2); Mermaid is a native subset (FR-028); math uses JLaTeXMath. Added resource targets (§10). Replaced the `web-host` module with `render-markdown`, `diagrams`, and `account`. OD-1 rewritten with an OS-web-view fallback.

Revision 2 (spec review, 2026-11-13) made these changes to the original draft:

- **Build and database (revision 2.1):** the project uses the Kotlin Toolchain (Amper), which cannot run Gradle plugins. SQLDelight was replaced with Room KMP, and Compose Desktop native distributions were replaced with executable JAR + `jpackage`. `shared` keeps an Android target as a portability guard, while `androidApp` is excluded from the build.
- **Platform decided: desktop.** Rewrote §8.2. Removed Readium Kotlin, which is Android-only. Added an embedded web engine for EPUB, Mermaid, and KaTeX, and a Milestone 0 spike.
- **Added a `SourceLocation` value object** (§7.0) with a text-quote anchor, so every artifact references the source the same way and survives file or renderer changes.
- **Data model fixes:** removed the duplicate `books.last_position`. Added content hash, storage mode, and tags to books. Added concept aliases, relationship types (inverse labels, symmetric flag), vocabulary occurrences, review step and history, understanding status, settings, and provider config. Replaced the single-row "AI interactions" with threads and messages, so follow-ups (FR-015) can be represented. Defined Markdown vs. SQLite source-of-truth rules, UUIDs, and the data directory layout.
- **Resolved conflicts:**
  - Per-request AI disclosure vs. "no excessive confirmations": replaced with one-time consent per provider plus a persistent indicator.
  - Review states differed between §4.3 and FR-038: unified as understanding markers vs. review outcomes.
  - Mermaid preview (P1) was in Milestone 5 and AI concept suggestions (P2) were inside the MVP milestones: milestones re-aligned to priorities.
  - Understanding markers, JSON export, and library search were missing from the milestones: added.
- **New requirements:** FR-043 (restore from export, which §13 already assumed), FR-044 (deletion semantics, which §9 required).
- **Made vague requirements testable:** context budget (FR-012), concrete spoiler rules (FR-023), lookup interaction and caching (FR-019), vocabulary dedup (FR-022), review scheduling outcomes (FR-040), desktop notification limits (FR-042), file import/relink (FR-002), export layout (FR-031), and performance targets (§10).
- **Added** secondary screens (§4.4), desktop layout and keyboard support (§6.4), Workflow F (§12), new risks (§13), and open decisions (§16).

---

# Final Product Principle

The application should not attempt to understand everything on behalf of the reader.

It should help the reader ask better questions, understand difficult ideas, preserve personal interpretations, and reconnect knowledge over time.

The book remains the source. The reader's notes remain their own. AI is an assistant, not the authority.

The MVP should prioritize a reliable reading experience and a durable personal knowledge store over sophisticated AI features or visualizations.
