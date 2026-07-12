plugins {
    `java-library`
}

val nvVersion: String by rootProject.extra

dependencies {
    api(project(":libraries:core"))
    implementation("com.neovisionaries:nv-websocket-client:$nvVersion")
}
