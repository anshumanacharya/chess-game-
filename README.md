# Local Chess Clock

A local (pass-and-play) two-player chess app for Android, built primarily for a
large ~2400×2200 tablet-class display lying flat on a table between two
players, with an **optional** chess clock: pick a time control and increment,
or play with unlimited time.

A plain HTML/JS build of the same rules engine is also hosted on GitHub
Pages for quick testing in a browser — see [Web test build](#web-test-build-github-pages)
below. The Android app is the actual target; the web build exists purely so
changes can be tried out without an Android device or emulator.

## Features

- Full chess rules: legal move generation, check/checkmate/stalemate, castling
  (both sides, with all the legality checks — can't castle out of, through, or
  into check), en passant, pawn promotion (choose queen/rook/bishop/knight),
  50-move rule, threefold repetition, and insufficient-material draws.
- Two players share one device — tap a piece, tap a highlighted destination.
- Optional chess clock:
  - **Unlimited time** — no clock at all.
  - **Timed** — pick a preset (1 min, 3|2, 5 min, 10 min, 15|10, 30 min) or a
    custom minutes-per-side + Fischer increment.
  - Clock pauses automatically if the app is backgrounded, and resumes when
    you come back (it doesn't drain your time while you're in another app).
- Fixed board orientation — White at the bottom, Black at the top — so the
  device can just lie flat on a table between the two players; the board
  itself never flips. Instead, every piece rotates 180° as a group each
  turn, so whoever is about to move sees the whole board, including the
  opponent's pieces, facing them the right way up.
- Each player's name, clock and captured-piece tray sit in a bar at their own
  end of the board (Black's above, White's below), rotated to face their
  seat. Unlike the pieces, that rotation is fixed per seat, not per turn —
  Black's bar never flips back and forth mid-game.
- Landscape, two-pane tablet layout: the board pane grows to fill all
  available height so the board is always as large as the screen allows; a
  side panel holds the (auto-scrolling) move list and the Resign / New setup
  controls, so they never compete with the board for space.
- A restrained, mostly-monochrome color scheme — warm-neutral board squares,
  one ink accent for interactive/active state, and a single muted red
  reserved for check — instead of a busy multi-color highlight palette.
- Resign, rematch (same time control), and "new setup" to change the clock.

## Project layout

This is a three-module Gradle project:

- **`chess-engine/`** — pure Kotlin (no Android dependency) chess rules engine
  and clock model. Fully unit-tested (`ChessGameTest`, `ClockStateTest`)
  covering move generation, castling, en passant, promotion, checkmates
  (fool's mate, scholar's mate), stalemate, insufficient material, and the
  clock's tick/increment/flag-fall behavior.
- **`app/`** — the Android app (Kotlin + Jetpack Compose + Material 3) that
  consumes `chess-engine` and renders the board, clocks, and setup screen. Its
  `GameViewModel` talks to the rules engine only through a small `GameSource`
  interface (`app/.../game/GameSource.kt`); `LocalGameSource` is today's
  on-device pass-and-play, wired in as the default. That seam exists so an
  online mode (e.g. backed by Lichess's Board API) can plug in later as
  another `GameSource` without changing the ViewModel or UI.
- **`web-engine/`** — a Kotlin/JS build of the *exact same* chess-engine
  source (shared via a Gradle `srcDir`, not copied) plus a small JS-facing
  facade (`JsApi.kt`: `JsGame`, `JsClock`) for the web test build below.
  `chess-engine` itself is untouched by this, so the Android app's dependency
  on it is unaffected.

Splitting the rules engine out like this means the hardest-to-get-right part
of the app (move legality) is plain Kotlin you can test with `gradle test`
without touching the Android toolchain at all — and, via `web-engine`, reuse
in a browser too.

## Web test build (GitHub Pages)

`web/` is a small hand-written HTML/CSS/JS page (`index.html`, `style.css`,
`app.js`, no framework or build step of its own) that mirrors the Android
app's behavior — fixed board orientation with per-turn piece rotation, the
same minimalist palette, player bars with clocks and captured pieces, move
list, promotion/resign/game-over overlays — driven by `web-engine`'s compiled
`chess-engine.js` bundle for all the actual rules and clock logic.

- **Try it locally**: open `web/index.html` directly in a browser — the
  compiled bundle is checked in, so no build step is required just to look at
  it.
- **Rebuild the engine bundle** after changing `chess-engine/` or
  `web-engine/`:
  ```bash
  ./gradlew :web-engine:nodeTest                 # run the shared test suite on the JS target
  ./gradlew :web-engine:browserProductionWebpack # produce web-engine/build/kotlin-webpack/js/productionExecutable/chess-engine.js
  cp web-engine/build/kotlin-webpack/js/productionExecutable/chess-engine.js web/chess-engine.js
  ```
- **Hosting**: `.github/workflows/deploy-pages.yml` rebuilds the bundle fresh
  from source and deploys `web/` to GitHub Pages on every push to `main` that
  touches `chess-engine/`, `web-engine/`, or `web/` (so the live site can
  never drift from what's committed). This needs the repository's
  **Settings → Pages → Source** set to **GitHub Actions** once, which I
  can't do from here — after that the workflow handles every future deploy.
- This is a testing convenience, not a second product: no framework, no
  responsive-design polish beyond a basic narrow-viewport fallback, and no
  attempt at pixel parity with the Android layout.

## Building the Android app

Open the project root in Android Studio (Jellyfish/Koala or newer) and let it
sync — it targets `compileSdk 34` / `minSdk 26` and uses AGP 8.5.2 with
Kotlin 2.0.21. The activity is locked to landscape and declared resizeable /
large-screen-friendly for its primary ~2400×2200 tablet target.

From the command line:

```bash
./gradlew :chess-engine:test   # run the rules-engine unit tests
./gradlew assembleDebug        # build the APK (requires the Android SDK)
```

> **Note on how this was built:** the sandbox this project was authored in
> has network access to Maven Central and the Gradle Plugin Portal, but not
> to Google's Maven repository (`dl.google.com`) or an installed Android SDK
> — both are required to resolve the Android Gradle Plugin / AndroidX /
> Compose dependencies and to compile/package the APK. Because of that,
> `:chess-engine` (pure Kotlin, no Android dependency) was actually compiled
> and its full test suite run and verified green in that environment, but
> `:app` could not be built or run there. The Compose UI code was written
> and reviewed carefully, but you should open it in Android Studio and do a
> normal build/run before considering it verified end-to-end.

## Project structure

```
chess-engine/
  src/main/kotlin/com/chessapp/engine/
    Color.kt, PieceType.kt, Piece.kt, Square.kt, Move.kt, CastlingRights.kt
    Board.kt, GameState.kt, GameStatus.kt, MoveGenerator.kt, ChessGame.kt
    ClockConfig.kt, ClockState.kt
  src/test/kotlin/com/chessapp/engine/
    ChessGameTest.kt, ClockStateTest.kt

app/
  src/main/kotlin/com/chessapp/localclock/
    MainActivity.kt
    game/GameSource.kt              # LocalGameSource today; a future online source plugs in here
    viewmodel/GameViewModel.kt, GameUiState.kt
    ui/screens/SetupScreen.kt, GameScreen.kt
    ui/components/ChessBoard.kt, ClockDisplay.kt, CapturedPiecesRow.kt,
                  PromotionDialog.kt, GameOverDialog.kt, MoveHistoryList.kt,
                  PieceGlyph.kt
    ui/theme/Theme.kt

web-engine/                          # Kotlin/JS, shares chess-engine's src/main/kotlin
  src/main/kotlin/JsApi.kt           # JsGame / JsClock — the only web-specific engine code
  src/test/kotlin/                   # EngineOnJsTest.kt (ported), JsApiTest.kt

web/                                 # the static site GitHub Pages serves
  index.html, style.css, app.js
  chess-engine.js                    # compiled from web-engine; regenerated by CI on every deploy

.github/workflows/deploy-pages.yml
```

## Design notes

- The board renders Unicode chess glyphs (♔♕♖♗♘♙ / ♚♛♜♝♞♟) rather than
  bitmap piece art, so there are no image assets to ship or license.
- `GameState` is immutable — every move produces a new snapshot — which is
  what makes the engine easy to unit test and keeps the UI layer simple
  (no aliasing bugs from a shared mutable board).
- The clock is modeled the same way (`ClockState.tick(elapsedMillis)` /
  `onMoveCompleted(...)`), driven by a 100ms coroutine ticker in
  `GameViewModel`, and is paused/resumed with the Activity lifecycle.
- `web-engine`'s tests don't reuse `chess-engine`'s test *files* directly
  (only its production source): Kotlin/JS test function names can't contain
  spaces, and chess-engine's JVM tests are named with backtick-quoted natural
  language (`` `castling is illegal through check` ``). `EngineOnJsTest`
  covers the same scenarios under plain camelCase names instead.
- The web build's `app.js` only patches the two clock displays in place on
  each 100ms tick (`updateClockDisplays()`); a full re-render only happens on
  an actual game-state change (a move, promotion, resign, game over). An
  earlier version re-rendered the whole board on every tick, which a
  Playwright-driven click could race against — worth keeping in mind if
  you're tempted to simplify that back to one `render()` call.
