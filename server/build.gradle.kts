plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

group = "de.pr.loaf.software"
version = "unspecified"

repositories {
    mavenCentral()
}
val krpcManifestDirectory =
    layout.buildDirectory.dir("krpc/generated/manifest")

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-P")
        freeCompilerArgs.add(
            "plugin:dev.krpc.plugin:contractOutputDir=" +
                    krpcManifestDirectory.get().asFile.absolutePath
        )
    }
}


dependencies {
    implementation(project(":utils"))
    implementation(project(":dsl"))

    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.logbackClassic)

    testImplementation(kotlin("test"))

    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
}



tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "de.pr.loaf.software.server.ApplicationKt"
}