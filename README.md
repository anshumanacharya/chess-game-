# Local Chess

A local, two-player chess app for Android with an optional chess clock. Built
for a large tablet display, lying flat on a table between both players.

**Try it in a browser:** https://anshumanacharya.github.io/chess-game-/
(a JS build of the same rules engine, for quick testing — the Android app is
the real target.)

## Features

- Full chess rules: castling, en passant, promotion, and
  checkmate/stalemate/draw detection
- Optional clock: unlimited time, presets (1 min, 3|2, 5 min, 10 min, 15|10,
  30 min), or a custom time + increment
- Fixed board — White at the bottom, Black at the top — with all the pieces
  rotating 180° each turn so whoever's moving reads the board right-side up
- Each player's clock and captured pieces sit in a bar at their end of the
  board, rotated to face them
- Resign, offer/accept a draw, or start a rematch
- Optional single-player mode against a built-in bot (a rough ~1000 Elo
  design target, not a calibrated rating)
- A minimalist, mostly-monochrome color scheme

## Project layout

- `chess-engine/` — the chess rules and clock logic, pure Kotlin, unit-tested
- `app/` — the Android app (Jetpack Compose)
- `web-engine/` — compiles `chess-engine` (and the bot, see below) to
  JavaScript for the web build
- `web/` — the static site (plain HTML/CSS/JS) hosted on GitHub Pages
- `chess-bot/` — a git submodule pointing at
  [chess-game-bot](https://github.com/anshumanacharya/chess-game-bot), a
  separate repo where the single-player bot's search/evaluation/difficulty
  tuning is developed. Every build here (Android and web) compiles against
  that repo's latest `main`, not a pinned commit — see `updateBotSubmodule`
  in `build.gradle.kts`.

## Building

Clone with `--recurse-submodules` (or run `git submodule update --init`
afterward) so `chess-bot/` is populated.

**Android** — open the project in Android Studio and run it, or:

```bash
./gradlew :chess-engine:test   # rules-engine unit tests
./gradlew assembleDebug        # build the APK
```

**Web** — `web/index.html` works standalone (the compiled engine is checked
in). To rebuild it after changing the engine:

```bash
./gradlew :web-engine:browserProductionWebpack
cp web-engine/build/kotlin-webpack/js/productionExecutable/chess-engine.js web/chess-engine.js
```

Pushing to `main` auto-deploys the web build via GitHub Actions.
