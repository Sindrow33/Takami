pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Takami"
include(":app")
include(":core-design", ":core-model", ":core-data", ":core-source")
include(":feature-library", ":feature-title", ":feature-calendar", ":feature-stats")
include(":reader-manga", ":player-anime", ":reader-novel")
