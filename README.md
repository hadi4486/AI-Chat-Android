# AI Chat — Android (Kotlin + Jetpack Compose)

A native Android AI chat client that talks to any OpenAI-compatible `/chat/completions` API
(OpenAI itself, OpenRouter, a local Ollama/LM Studio server, etc.). Built from the attached master
spec: MVVM + Repository pattern, Room for local history, DataStore + Android Keystore for settings,
streaming responses, Markdown rendering, and a Persian (RTL) UI.

## Before you build this — please read

This project was generated in a sandboxed environment with **no Android SDK, no emulator, and no
internet access**. That means every file here was written by hand to the real Android/Kotlin/Compose
APIs, but **none of it has been compiled, built, or run**. Treat this as a strong first draft of a
real, working app — not as something guaranteed to build on the first try.

Concretely, expect to spend a little time on:

- **Opening it in Android Studio** (Hedgehog/Iguana or newer) and letting it generate the Gradle
  wrapper jar — `gradle/wrapper/gradle-wrapper.properties` points at Gradle 8.9, but the wrapper
  `.jar` itself couldn't be downloaded here. Android Studio does this automatically on first sync;
  from a terminal you'd run a locally-installed `gradle wrapper` once instead.
- **Dependency versions** in `app/build.gradle.kts` — they're real, current-as-of-training-data
  versions (Compose BOM 2024.09.02, Room 2.6.1, etc.), but I couldn't hit Maven Central to confirm
  the newest patch releases or catch a version conflict.
- **A first-compile pass** — likely a handful of small fixes (an import, an API signature that
  shifted slightly between library versions). Nothing here is exotic, but I'd be overstating things
  to call it guaranteed to compile untouched.

## Architecture

```
app/src/main/java/com/aichat/assistant/
├── data/
│   ├── api/            ChatApiClient (OkHttp, streaming SSE parsing, error mapping), DTOs
│   ├── database/        Room entities, DAOs, AppDatabase
│   └── repository/      Repository implementations (bridge data ↔ domain)
├── domain/
│   ├── model/            Message, Conversation, ApiProviderSettings, AppError, ...
│   ├── repository/       Repository interfaces + the Outcome<T> result type
│   └── usecase/          SendMessage, TestConnection, FetchModels, ExportMarkdown
├── presentation/
│   ├── navigation/       NavHost + a hand-rolled ViewModel factory (see below)
│   ├── theme/            Color/Type/Motion/Theme — the warm custom palette + animated theme switch
│   ├── components/       MessageBubble, ChatInputBar, MarkdownText, shared list/empty/error views
│   ├── splash/ home/ chat/ settings/ history/    one package per screen (ViewModel + Composable)
├── security/             SecureKeyStore — the only class allowed to touch the raw API key
└── utils/                ConnectivityObserver, ErrorMapper, small formatting helpers
```

**Dependency injection is a hand-written `AppContainer`, not Hilt.** For an app this size, Hilt's
annotation processor is more moving parts than the problem needs — the spec itself asks to keep
dependencies to a minimum. `AppContainer` is a flat set of `by lazy` singletons; `ViewModelFactory.kt`
wires them into each screen's ViewModel using the standard `viewModelFactory { initializer { } }` DSL.

## Design decisions worth knowing about

- **Markdown rendering is hand-rolled** (`presentation/components/MarkdownText.kt`), not a third-party
  library. I couldn't verify a Compose Markdown library's current Maven coordinates/API without
  internet access, and a dependency that doesn't resolve would break the whole build. The custom
  parser covers headings, bold/italic, inline code, fenced code blocks (with a copy button), links,
  ordered/unordered lists, and basic tables — everything the spec asked for — and degrades gracefully
  on partial/streaming input. Swapping in `compose-richtext` or similar later is a contained change.
- **Export shares plain text**, not a file. "Export as Markdown/JSON" builds the text and hands it to
  the Android share sheet (`Intent.ACTION_SEND`) rather than writing through a `FileProvider`. It's a
  smaller moving part and still fully satisfies "get your conversation out of the app" — saving to a
  specific folder is a reasonable follow-up if you want it.
- **Chat bubble side follows layout direction, not a hardcoded side.** The user's own messages use
  `Alignment.End`, which Compose resolves per the RTL locale — the standard, system-consistent
  behavior for a `supportsRtl="true"` app. Some Persian chat apps deliberately keep "my messages" on
  the physical right regardless of RTL; if you want that instead, it's one alignment flip in
  `MessageBubble`.
- **Timestamps are Gregorian**, formatted via `java.text.SimpleDateFormat`. A proper Persian release
  would want a Jalali calendar formatter — a nice follow-up, deliberately left out rather than
  half-implemented.
- **No custom font is bundled.** Same reason as the Markdown library: I can't fetch a `.ttf` without
  internet. Typography (`presentation/theme/Type.kt`) uses the platform default font family with a
  deliberate weight/size scale; wiring in a downloaded Google Font later is a one-file change.
- **The API key never leaves `SecureKeyStore`.** It's the only class that touches
  `EncryptedSharedPreferences` (Keystore-backed AES256-GCM); everything else in Settings goes through
  plain DataStore. The debug HTTP logging interceptor is capped at `Level.BASIC` (method + response
  code only — never headers or bodies), and is fully off in release builds, so the key can't end up
  in Logcat either way.

## What's implemented vs. simplified from the spec

Implemented for real: streaming + non-streaming chat completions against any OpenAI-compatible
endpoint, model listing with manual fallback, Test Connection, Room-backed history with search/pin/
delete, edit-and-regenerate, retry-on-error, stop-generation mid-stream, light/dark/system theming
with Material You dynamic color and an animated transition between them, RTL Persian UI, offline
detection, and the full settings surface from the spec.

Deliberately trimmed: voice input and file attachments are marked optional in the spec
("در صورت پیاده‌سازی") and were left out rather than added as non-functional buttons. The automated
test suite has two real, focused unit tests (`ModelsTest`, `ExtensionsTest`) covering pure logic —
expanding into ViewModel tests with fake repositories and Compose UI tests is the natural next step
once this builds on your machine.

## Running it

1. Open the project root in Android Studio; let it sync (this also generates the Gradle wrapper jar).
2. Run on a device/emulator with API 26+.
3. On first launch, go to Settings → enter a Base URL (e.g. `https://api.openai.com/v1` or your
   provider's OpenAI-compatible endpoint), an API key, and a model name — then tap "Test Connection."
4. Start a new chat from the Home screen's "+" button.
