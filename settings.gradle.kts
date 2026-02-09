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
    val githubMavenUrl = System.getenv("GITHUB_MAVEN_URL")
        ?: providers.gradleProperty("GITHUB_MAVEN_URL").orNull

    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories {
            google()
            mavenCentral()

            if (githubMavenUrl != null) {
                maven {
                    name = "GitHub"
                    url = uri(githubMavenUrl)
                    credentials {
                        username = githubUsername
                        password = githubToken
                    }
                }
            }
        }
    }
}

rootProject.name = "LayerDesignSystem"
include(":app")
include(":designsystem")
