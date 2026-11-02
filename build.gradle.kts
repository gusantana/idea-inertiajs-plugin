import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.intellij.platform") version "2.19.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.5.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "2026.2.2"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.9.11"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Set the JVM language level used to build the project. IntelliJ Platform 2025.1+ requires Java 21.
kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.PhpStorm, properties("platformVersion"))

        // Bundled plugins, see `platformBundledPlugins` in gradle.properties.
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        )

        // Marketplace plugins, see `platformPlugins` in gradle.properties.
        plugins(
            providers.gradleProperty("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        )

        // Provides the Java instrumentation agent used by `instrumentCode`.
        // `javaCompiler()` is applied by default by the IntelliJ Platform Gradle Plugin 2.x.

        // Test framework for IntelliJ Platform based tests.
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(file(".qodana").canonicalPath)
    resultsPath.set(file("build/results/inspections").canonicalPath)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover
kover {
    reports {
        total {
            xml {
                onCheck.set(true)
            }
        }
    }
}

// Configure the IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    buildSearchableOptions.set(false)

    pluginConfiguration {
        id.set(properties("pluginGroup"))
        name.set(properties("pluginName"))
        version.set(properties("pluginVersion"))

        ideaVersion {
            sinceBuild.set(properties("pluginSinceBuild"))
            untilBuild.set(properties("pluginUntilBuild"))
        }

        // Extract the <!-- Plugin description --> section from README.md and provide it for the plugin's manifest
        description.set(
            provider {
                file("README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").let { markdownToHTML(it) }
            }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                with(changelog) {
                    renderItem(
                        getOrNull(properties("pluginVersion"))
                            ?: runCatching { getLatest() }.getOrElse { getUnreleased() },
                        Changelog.OutputType.HTML,
                    )
                }
            }
        )
    }

    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(
            listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
        )
    }

    pluginVerification {
        ides {
            current()
        }
    }
}

// Configure UI tests plugin
// Read more: https://github.com/JetBrains/intellij-ui-test-robot
val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        }
    }

    plugins {
        robotServerPlugin()
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}
