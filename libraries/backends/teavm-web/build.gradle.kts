plugins {
    `java-library`
}

val gdxTeaVMVersion: String by rootProject.extra

dependencies {
    api(project(":libraries:core"))
    implementation("com.github.xpenatan.gdx-teavm:backend-web:$gdxTeaVMVersion")
}
