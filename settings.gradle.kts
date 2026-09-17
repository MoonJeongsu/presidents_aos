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
        maven {
            url = uri("https://cauly.github.io/cauly-sdk-android-maven/maven-repo")
        }
    }
}

rootProject.name = "PresidentialSpeeches"
include(":app")
