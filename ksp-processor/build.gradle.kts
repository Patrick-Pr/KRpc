plugins {
    kotlin("jvm")
}

group = "de.pr.loaf.software"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":contract-model"))
    implementation(libs.kspSymbolProcessingApi)
    implementation(libs.kotlinxSerialization)

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}