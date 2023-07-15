val lwjglVersion = "3.2.3"
val lwjglNatives = "natives-windows"

dependencies {
//    implementation("com.github.exerosis:mynt:1.1.0")
    implementation(project(":Sockets"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.netty:netty-all:4.1.94.Final")
    implementation("org.joml:joml:1.9.23")
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    implementation(kotlin("stdlib-jdk8"))
}

val outDir = project.properties["outdir"] as? String ?: "./out"

tasks.shadowJar {
    println("Destination directory: $outDir")
    archiveFileName.set("Client.jar")
    destinationDirectory.set(file(outDir))
    manifest.attributes["Main-Class"] = "me.mason.client.MainKt"
}