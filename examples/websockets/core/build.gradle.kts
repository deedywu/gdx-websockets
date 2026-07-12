plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api(project(":libraries:core"))
}
