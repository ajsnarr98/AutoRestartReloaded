pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions("1.20.1-fabric", "1.21.1-fabric", "1.21.10-fabric", "1.21.10-neoforge")
        vcsVersion = "1.21.10-neoforge"
    }
}

rootProject.name = "Auto Restart Reloaded"