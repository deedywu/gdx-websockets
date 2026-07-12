import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("sourcepath", "")
}

dependencies {
    api(project(":libraries:core"))
    implementation("com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion:sources")
    implementation("com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion:sources")
}
