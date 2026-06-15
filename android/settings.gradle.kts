pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Antimatter"
include(":app")
include(":core:data")
include(":core:network")
include(":core:ui")
include(":feature:connect")
include(":feature:chat")
include(":feature:files")
include(":feature:terminal")

include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("terminal-emulator")

include(":terminal-view")
project(":terminal-view").projectDir = file("terminal-view")
