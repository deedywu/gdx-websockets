plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra
val teavmVersion: String by rootProject.extra

dependencies {
    api(project(":libraries:core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.teavm:teavm-interop:$teavmVersion")
}
