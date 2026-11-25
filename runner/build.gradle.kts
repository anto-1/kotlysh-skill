plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.kotlysh:kotlysh-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

kotlin {
    jvmToolchain(21)
}

// The launcher copies the task to src/main/kotlin/Task.kt, builds it, then
// starts it itself so that arguments, stdin and the exit code pass through.
tasks.register("writeClasspath") {
    val classpath = sourceSets.main.get().runtimeClasspath
    val target = layout.buildDirectory.file("classpath.txt")
    inputs.files(classpath)
    outputs.file(target)
    doLast { target.get().asFile.writeText(classpath.asPath) }
}
