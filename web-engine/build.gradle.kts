plugins {
    id("org.jetbrains.kotlin.js")
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                outputFileName = "chess-engine.js"
            }
        }
        nodejs()
    }

    sourceSets {
        val main by getting {
            // Reuses the exact same rules-engine source files chess-engine compiles for the
            // Android app's JVM target — one source of truth for chess rules, compiled twice.
            // chess-engine itself is left untouched so the Android app's dependency on it is
            // unaffected by this module.
            kotlin.srcDir("../chess-engine/src/main/kotlin")
            // The bot's own code — developed in a separate repo, pulled in as source via the
            // chess-bot submodule, same trick as chess-engine above.
            kotlin.srcDir("../chess-bot/src/main/kotlin")
        }
        val test by getting {
            // Not shared with chess-engine's own test source: Kotlin/JS test function names
            // can't contain spaces, so chess-engine's `` `natural language` `` JVM test names
            // don't compile here. EngineOnJsTest below covers the same critical scenarios under
            // JS-legal names instead.
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Always compile against the bot's latest code — see updateBotSubmodule in the root build file.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    dependsOn(rootProject.tasks.named("updateBotSubmodule"))
}
