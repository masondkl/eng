dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

val outDir = project.properties["outdir"] as? String ?: "./out"

tasks.shadowJar {
    println("Destination directory: $outDir")
    archiveFileName.set("Server.jar")
    destinationDirectory.set(file(outDir))
    manifest.attributes["Main-Class"] = "me.mason.server.MainKt"
}