plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "26.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}

val releaseVersions = listOf(
    "26.1"
)

stonecutter tasks {
    order("publishModrinth")
    // order("publishCurseforge")
}

tasks.register("publishAllToModrinthRelease") {
    group       = "publishing"
    description = "Publish all release groups to Modrinth sequentially."
    dependsOn(releaseVersions.map { ":$it:publishModrinth" })
}

/*
tasks.register("publishAllToCurseforgeRelease") {
    group       = "publishing"
    description = "Publish all release groups to CurseForge sequentially."
    dependsOn(releaseVersions.map { ":$it:publishCurseforge" })
}
*/

tasks.register("publishToAllPlatforms") {
    group       = "publishing"
    description = "Publishes to both Modrinth and Curseforge sequentially."
    dependsOn("publishAllToModrinthRelease")//, "publishAllToCurseforgeRelease")
}

gradle.projectsEvaluated {
    releaseVersions.zipWithNext().forEach { (prev, next) ->
        project(":$next").tasks.named("publishModrinth") {
            mustRunAfter(":$prev:publishModrinth")
        }
        /*
        project(":$next").tasks.named("publishCurseforge") {
            mustRunAfter(":$prev:publishCurseforge")
        }
        */
    }
}