plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    api(project(":libraries:core"))
    testImplementation("junit:junit:4.13.2")
}
