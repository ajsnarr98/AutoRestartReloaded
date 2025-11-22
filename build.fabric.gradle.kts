plugins {
    id("fabric-loom")

    // `maven-publish`
    // id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}-fabric"
base.archivesName = property("mod.id") as String
//group = property("") as String

val requiredJava = when {
    stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
    stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
    stonecutter.eval(stonecutter.current.version, ">=1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, property("deps.fabric-api") as String))
    }

    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment"))
            parchment("org.parchmentmc.data:parchment-${property("deps.parchment-mc-version")}:${property("deps.parchment-mappings")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")

    fapi("fabric-lifecycle-events-v1", "fabric-resource-loader-v0", "fabric-content-registries-v0")

    implementation("com.cronutils:cron-utils:${property("deps.cronutils")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml")

        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft_version_range", project.property("deps.modloader-mc-version-range"))
        inputs.property("fabric", project.property("deps.fabric-loader-range"))
        inputs.property("neoforge", project.property("deps.neoforge"))
        inputs.property("license", project.property("mod.license"))
        inputs.property("author", project.property("mod.author"))
        inputs.property("description", project.property("mod.description"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.id"),
            "version" to project.property("mod.version"),
            "minecraft_version_range" to project.property("deps.modloader-mc-version-range"),
            "fabric" to project.property("deps.fabric-loader-range"),
            "neoforge" to project.property("deps.neoforge"),
            "license" to project.property("mod.license"),
            "author" to project.property("mod.author"),
            "description" to project.property("mod.description"),
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

// later, look back at this example
// https://github.com/murderspagurder/mod-template-java/blob/main/build.fabric.gradle.kts

/*
// Publishes builds to Modrinth and Curseforge with changelog from the CHANGELOG.md file
publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
        || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }
}
 */
/*
// Publishes builds to a maven repository under `com.example:template:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            // To authenticate, create `myMavenUsername` and `myMavenPassword` properties in your Gradle home properties.
            // See https://stonecutter.kikugie.dev/wiki/tips/properties#defining-properties
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
