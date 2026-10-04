pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // lielugit-updater : dépôt Maven public vendoré (issu de lielugit-updater-<version>-maven.zip),
        // versionné dans git : aucun jeton ni secret, local comme CI.
        maven {
            url = uri("$rootDir/libs/lielugit-maven")
            content { includeGroup("com.lielu") }
        }
    }
}

rootProject.name = "Messous"

include(":app")
include(":core-engine")
