plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

val contractsDirectory = layout.buildDirectory.dir("krpc/generated/contracts")
val serverManifest = project(":server").layout.buildDirectory.dir("krpc/generated/manifest/contract_output.json")
ksp {
    arg("krpc.contractDirectory", contractsDirectory.get().asFile.absolutePath)
    arg("krpc.manifest", serverManifest.get().asFile.absolutePath)
}
tasks.matching { it.name == "kspKotlin" }.configureEach {
    dependsOn(":server:compileKotlin")
    inputs.file(serverManifest).withPathSensitivity(PathSensitivity.RELATIVE)
}

kotlin {
    jvmToolchain(25)

//    compilerOptions {
//        freeCompilerArgs.add("-Xverify-ir")
//    }
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    ksp(project(":ksp-processor"))
    implementation(project(":utils"))
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.logbackClassic)

    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    testImplementation(kotlin("test"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "de.pr.loaf.software.app.AppKt"
}
