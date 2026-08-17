plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

kotlin {
    jvmToolchain(25)
//    compilerOptions {
//        freeCompilerArgs.add("-Xverify-ir")
//    }
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.logbackClassic)

    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    testImplementation(kotlin("test"))

    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "de.pr.loaf.software.app.AppKt"
}
