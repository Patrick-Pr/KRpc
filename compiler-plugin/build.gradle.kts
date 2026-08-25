plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":contract-model"))
    implementation(libs.kotlinxSerialization)

    compileOnly(libs.kotlinCompilerEmbeddable)
}