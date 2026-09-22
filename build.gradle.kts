plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.js") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// The bot's own decision-making code lives in the chess-bot submodule
// (https://github.com/anshumanacharya/chess-game-bot), developed independently of this repo.
// This task moves the submodule to the tip of its tracked branch (not whatever commit happens
// to be pinned in this repo's tree) before every build, so both the Android app and the web
// build always compile against the bot's latest code. :app and :web-engine's compile tasks
// depend on this — see their build.gradle.kts.
tasks.register<Exec>("updateBotSubmodule") {
    workingDir = rootDir
    commandLine("git", "submodule", "update", "--remote", "chess-bot")
}
