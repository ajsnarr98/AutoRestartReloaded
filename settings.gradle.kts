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
        fun bothLoaders(vararg versions: String) {
            val loaders = listOf("neoforge", "fabric")
            for (loader in loaders) {
                for (version in versions) {
                    version("$version-$loader", version).buildscript = "build.$loader.gradle.kts"
                }
            }
        }

        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        bothLoaders("1.21.10")

        vcsVersion = "1.21.10-neoforge"
    }
}

rootProject.name = "Auto Restart Reloaded"