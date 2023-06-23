import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

val lwjglVersion = "3.2.3"
val lwjglNatives = "natives-windows"

group = "me.mason"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral(); mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
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
    archiveFileName.set("${project.name}.jar")
    destinationDirectory.set(file(outDir))
    manifest.attributes["Main-Class"] = "me.mason.crank.MainKt"
}

tasks.compileKotlin.get().kotlinOptions {
    languageVersion = "1.7"
    jvmTarget = "17"
    freeCompilerArgs = listOf(
        "-version",
        "-Xcontext-receivers",
        "-Xskip-prerelease-check",
        "-Xinline-classes",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi"
    )
}