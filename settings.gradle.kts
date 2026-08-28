rootProject.name = "anime-scene-search"

pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories { mavenCentral() }
}

include(":imagesearch-api")
include(":imagesearch-core")
include(":imagesearch-resolver")
