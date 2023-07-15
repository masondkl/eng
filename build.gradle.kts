plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

allprojects {
    group = "me.mason"
    version = "1.0.0"
    repositories {
        mavenCentral(); mavenLocal()
        maven("https://jitpack.io")
    }
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")
    tasks.compileKotlin.get().kotlinOptions {
        languageVersion = "1.7"
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xcontext-receivers", "-Xinline-classes",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-Xopt-in=kotlinx.coroutines.InternalCoroutinesApi"
        )
    }
}