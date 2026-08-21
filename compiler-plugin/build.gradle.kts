plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":app"))
    implementation(libs.kotlinxSerialization)

    compileOnly(libs.kotlinCompilerEmbeddable)
}