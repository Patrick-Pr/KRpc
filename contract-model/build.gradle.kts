plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "de.pr.loaf.software"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinxSerialization)

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}