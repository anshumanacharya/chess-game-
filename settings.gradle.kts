pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_PROJECT (Gradle's default; was FAIL_ON_PROJECT_REPOS): the Kotlin/JS plugin
    // registers its own Node.js/Yarn distribution repositories at configuration time for
    // :web-engine, which FAIL_ON_PROJECT_REPOS rejects outright and PREFER_SETTINGS silently
    // ignores (downgrades the error to a warning but still never resolves the artifact).
    // PREFER_PROJECT lets that project-declared repo be consulted normally alongside these.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LocalChessClock"
include(":app")
include(":chess-engine")
include(":web-engine")
