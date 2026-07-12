import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
    `java-library`
}

val appName: String by rootProject.extra
val gdxVersion: String by rootProject.extra

configurations {
    create("custom")
}

configurations.matching { it.name == "compile" }.configureEach {
    extendsFrom(configurations["custom"])
}

extensions.configure<EclipseModel> {
    project.name = "$appName-core"
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}

extra["ARTIFACTID"] = "core"
