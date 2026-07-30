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

rootProject.name = "nanfeng-bazi"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:engine-tyme")
include(":core:solar-time")
