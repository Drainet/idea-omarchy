import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform")
}

group = "org.omarchy"
version = "0.1.0-SNAPSHOT"

val androidStudioVersion = providers.gradleProperty("androidStudioVersion").get()
val intellijIdeaVersion = providers.gradleProperty("intellijIdeaVersion").get()

kotlin {
    // Android Studio 2026.1 ships a Java 21 runtime.  Keep the plugin bytecode
    // loadable there even when Gradle itself uses a newer JDK.
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.tomlj:tomlj:1.1.1")

    intellijPlatform {
        // Compile against the oldest supported product. The plugin only uses
        // com.intellij.modules.platform APIs, so this keeps it portable across
        // Android Studio and newer IntelliJ IDEA releases.
        androidStudio(androidStudioVersion)
        pluginVerifier()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

intellijPlatform {
    pluginConfiguration {
        name = "Omarchy Theme Sync"
        version = project.version.toString()
        description = "Synchronizes JetBrains IDE UI and editor colors with the active Omarchy palette."
        vendor {
            name = "Omarchy Community"
            url = "https://omarchy.org"
        }
        ideaVersion {
            sinceBuild = "261"
        }
    }

    pluginVerification {
        ides {
            current()
            create(IntelliJPlatformType.IntellijIdea, intellijIdeaVersion)
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
    test {
        useJUnitPlatform()
    }
}
