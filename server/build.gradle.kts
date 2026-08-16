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

dependencies {
    implementation(project(":utils"))

    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.logbackClassic)

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "de.pr.loaf.software.server.ApplicationKt"
}