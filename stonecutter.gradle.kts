plugins {
    id("dev.kikugie.stonecutter")
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    id("fabric-loom") version "1.13-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.119" apply false
    // id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.10-neoforge"

/*
stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
 */

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    swaps["modid"] = "\"${property("mod.id")}\";"
//    constants["release"] = property("mod.id") != "template"
//    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
