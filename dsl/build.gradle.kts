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
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.logbackClassic)

    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}