# Local Chess Clock

A local (pass-and-play) two-player chess app for Android, built primarily for a
large ~2400×2200 tablet-class display lying flat on a table between two
players, with an **optional** chess clock: pick a time control and increment,
or play with unlimited time.

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

This is a two-module Gradle project:

- **`chess-engine/`** — pure Kotlin (no Android dependency) chess rules engine
  and clock model. Fully unit-tested (`ChessGameTest`, `ClockStateTest`)
  covering move generation, castling, en passant, promotion, checkmates
  (fool's mate, scholar's mate), stalemate, insufficient material, and the
  clock's tick/increment/flag-fall behavior.
- **`app/`** — the Android app (Kotlin + Jetpack Compose + Material 3) that
  consumes `chess-engine` and renders the board, clocks, and setup screen.

Splitting the rules engine out like this means the hardest-to-get-right part
of the app (move legality) is plain JVM code you can test with `gradle test`
without touching the Android toolchain at all.

## Building

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
    viewmodel/GameViewModel.kt, GameUiState.kt
    ui/screens/SetupScreen.kt, GameScreen.kt
    ui/components/ChessBoard.kt, ClockDisplay.kt, CapturedPiecesRow.kt,
                  PromotionDialog.kt, GameOverDialog.kt, MoveHistoryList.kt,
                  PieceGlyph.kt
    ui/theme/Theme.kt
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
