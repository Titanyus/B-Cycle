pluginManagement {
    repositories {
        maven("https://dl.google.com/dl/android/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://dl.google.com/dl/android/maven2/")
        mavenCentral()
    }
}
rootProject.name = "B-Cycle"
include(":app", ":hrv-core")
