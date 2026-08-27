import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform")
}

group = "org.omarchy"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.tomlj:tomlj:1.1.1")

    intellijPlatform {
        local(file("/home/drain/jetbrains/idea-IU-262.9437.185"))
        pluginVerifier()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

intellijPlatform {
    pluginConfiguration {
        name = "Omarchy Theme Sync"
        version = project.version.toString()
        description = "Synchronizes IntelliJ IDEA's UI and editor colors with the active Omarchy palette."
        vendor {
            name = "Omarchy Community"
            url = "https://omarchy.org"
        }
        ideaVersion {
            sinceBuild = "262"
        }
    }

    pluginVerification {
        ides {
            local(file("/home/drain/jetbrains/idea-IU-262.9437.185"))
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }
    test {
        useJUnitPlatform()
    }
}
