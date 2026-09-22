# CLAUDE.md

## Overview & Stack

Local, two-player chess app for Android (Jetpack Compose) with an optional
clock, plus a browser test build (Kotlin/JS → static site on GitHub Pages)
sharing the same Kotlin rules engine. Optional single-player mode against a
bot developed in a separate repo, pulled in as a git submodule.

## Commands

Clone with `--recurse-submodules` (or `git submodule update --init` after) so
`chess-bot/` is populated — required for any build.

```bash
./gradlew :chess-engine:test               # rules-engine unit tests (JVM)
./gradlew :web-engine:nodeTest             # same tests, JS target (what CI runs)
./gradlew assembleDebug                    # build the Android debug APK
./gradlew :web-engine:browserProductionWebpack   # rebuild the JS engine bundle
cp web-engine/build/kotlin-webpack/js/productionExecutable/chess-engine.js web/chess-engine.js
```

No Android SDK / `dl.google.com` access in this sandbox — Android code
cannot be compiled or run here. Verify Android changes by careful manual
read-through (and cross-checking generated resources, e.g. diffing
VectorDrawable `pathData` against source SVGs char-for-char) rather than
claiming a build passed. Flag this caveat to the user whenever Android
changes go out unverified by a real build.

`web/index.html` works standalone (the compiled engine is checked in).
Pushing to `main` auto-deploys the web build via `.github/workflows/deploy-pages.yml`
— but its `paths` filter only covers `chess-engine/**`, `web-engine/**`,
`web/**`, and the workflow file itself, so **Android-only changes (`app/**`)
never trigger it**; only touch/verify that workflow when `web/` or the
shared engine changed.

## Code Architecture & Conventions

- `chess-engine/` — pure Kotlin rules + clock logic (`Board`, `ChessGame`,
  `MoveGenerator`, `ClockState`, etc.), unit-tested, shared by both platforms.
- `app/` — Android (Jetpack Compose). `viewmodel/GameViewModel` +
  `GameUiState` drive `ui/screens/GameScreen.kt`; `game/GameSource` is an
  abstraction point for a future online mode (e.g. Lichess Board API)
  without touching the ViewModel or UI. `bot/` holds `BotSource` /
  `RemoteBotSource` (runs the bot via WebView, online-first with an offline
  fallback bundled copy — see `BotSource.kt`/`ConnectivityChecker.kt`).
- `web-engine/` — compiles `chess-engine` (+ the bot) to JS for the browser.
- `web/` — static site (plain HTML/CSS/JS) consuming the compiled bundle;
  `web/pieces/` holds source SVGs (Cburnett set, CC BY-SA 3.0).
- `chess-bot/` — git submodule → `chess-game-bot` repo. Every build compiles
  against that repo's latest `main`, not a pinned commit (`updateBotSubmodule`
  in `build.gradle.kts`) — a bot-only push there doesn't trigger this repo's
  CI, so `deploy-pages.yml` also runs on a daily cron to keep the live site
  current.
- **Visual parity, not mechanism parity, across platforms**: when porting a
  UI change between web and Android, replicate the *outcome* using each
  platform's native idiom (e.g. web's CSS-Grid side-panel-height trick has
  no Android equivalent because Android never had that layout problem) —
  don't blindly port implementation details, and don't port web-specific bug
  fixes whose bug can't occur in Compose (e.g. a manual class-list reset
  losing a CSS rotation class has no Compose analogue).
- Board/piece colors are Compose `Color` constants in
  `ui/theme/Theme.kt` (`BoardLightSquare`, `BoardDarkSquare`,
  `BoardLegalTarget`, etc.) mirrored by CSS custom properties in
  `web/style.css` (`--board-light`, `--board-dark`, `--legal-target`, …) —
  keep these two in sync by hex value when changing board theme.
  Current theme: lichess.org's default ("brown") board
  (`#F0D9B5`/`#B58863`), dark near-black legal-move dot (`#141E0A`).
- Pieces render as real Cburnett artwork on both platforms, not Unicode
  glyphs: `web/pieces/*.svg` on web, hand-converted
  `app/src/main/res/drawable/piece_*.xml` VectorDrawables on Android
  (via `ui/components/PieceIcon.kt`'s `pieceIconRes()`).
- Board orientation: pass-and-play is fixed (White bottom/Black top) with
  pieces rotating 180° on Black's turn; playing the bot as Black instead
  *flips the board* (like lichess/chess.com) so the human's pieces stay at
  the bottom — implemented as `boardFlipped` in `ChessBoard.kt` remapping
  `(file,rank)` → `(displayRow,displayCol)`, not a piece-rotation hack.
- No per-player name labels in the clock UI on either platform — position
  (top/bottom) plus the adjacent captured-pieces' color already disambiguate
  whose clock/bar is whose. Captured pieces + classic `+N` material
  advantage render directly beside each clock (`CapturedPiecesRow.kt` /
  web equivalent), not in a separate name bar.

## Gotchas & Quirks

- **Android VectorDrawable has no `<circle>` primitive.** Converting an SVG
  piece with `<circle cx cy r>` (only `bQ.svg` among the current set) requires
  manually rewriting it as an arc path:
  `M (cx-r),cy A r,r 0 1,0 (cx+r),cy A r,r 0 1,0 (cx-r),cy Z`.
- **Android `<group>` does not inherit fill/stroke to child `<path>`** the
  way SVG's `<g fill=...>` does — every group-level style attribute (fill,
  stroke, stroke-width, line-cap/join) must be copied onto each individual
  `<path android:.../>` by hand when hand-converting SVG → VectorDrawable.
  SVG `fill="none"` → omit `android:fillColor` entirely; an SVG path with no
  explicit `fill` defaults to black per spec.
- Gradle `dependencyResolutionManagement.repositoriesMode` must stay
  `PREFER_PROJECT` (not the default-safer `FAIL_ON_PROJECT_REPOS` or
  `PREFER_SETTINGS`): the Kotlin/JS plugin registers its own Node/Yarn repos
  at configure time for `:web-engine`, which the stricter modes reject or
  silently fail to resolve.
- `lichess.org`/`lichess1.org` are unreachable from this sandbox's egress
  proxy; GitHub's code-search API 403s unauthenticated. Don't burn time
  retrying — use well-established general knowledge (e.g. lichess's known
  default board hex values) instead, and never use `mcp__github__*` tools
  against repos outside this session's configured GitHub scope to work
  around a network block.
- This session's actual configured GitHub scope can silently be a *different*
  repo (e.g. an unrelated `job-application-automation` project) from the one
  a stale/summarized task context describes. If working directory, repo
  content, and task summary disagree, stop and confirm with the user before
  continuing — don't assume the summary is right.
- CI (`deploy-pages.yml`) checks out with `submodules: true` (not
  `--recursive`) deliberately — the bot submodule's own nested submodule
  (a copy of *this* repo, used only so it can build standalone) isn't needed
  to build the site and would be wasted work to fetch.

## Current State & Next Steps

Completed this session:
- Fixed poor-contrast legal-move dots and adopted lichess's default board
  colors on both web and Android.
- Ported the recent web-only visual redesign to Android: real Cburnett piece
  artwork (replacing `PieceGlyph.kt`'s Unicode glyphs), board-flip when
  playing Black vs. the bot, removed name labels, captured pieces + material
  advantage next to each clock.
- Updated README credits to cover both platforms' artwork.
- Pushed to `main` (commit `5efc0ee`); `deploy-pages.yml` ran and succeeded.

Bot wiring (confirmed complete this session): `chess-bot` submodule has
`ChessBot`/`Evaluator`/`TwoPlySearch`/`PieceValues`/`BotConfig` + unit tests,
pulled in as a source dir (not a compiled dep) by both `app/` and
`web-engine/`. Android: `SetupScreen` → `GameViewModel` →
`bot/RemoteBotSource` (WebView, online-first with an offline fallback via
`ConnectivityChecker`). Web: `JsApi.kt` exposes `setBot`/`hasBot`/
`isBotTurn`/`playBotMove`, consumed by `app.js`'s `maybeTriggerBotMove`.
The stale in-progress task list in the repo (Gradle scaffolding era) is
outdated — bot integration is done on both platforms, not pending.

Open items:
- Android changes are unverified by an actual build/emulator run (sandbox
  has no Android SDK) — do a real on-device check next session.
