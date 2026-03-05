pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
gradle.settingsEvaluated {
    val githubUsername = System.getenv("GITHUB_USERNAME")
        ?: providers.gradleProperty("GITHUB_USERNAME").orNull
    val githubToken = System.getenv("GITHUB_TOKEN")
        ?: providers.gradleProperty("GITHUB_TOKEN").orNull
    val githubMavenUrl = "https://maven.pkg.github.com/squad3-layer/ds-squad03-layer"

    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://jitpack.io") }
            maven {
                name = "GitHub"
                url = uri(githubMavenUrl ?: System.getenv("GITHUB_MAVEN_URL"))
                credentials {
                    username = githubUsername
                    password = githubToken
                }
            }
        }
    }
}

rootProject.name = "LayerDesignSystem"
include(":app")
include(":designsystem")
