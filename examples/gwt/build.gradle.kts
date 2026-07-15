import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync

plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra
val gwtVersion: String by rootProject.extra

val gwtCompiler by configurations.creating
val serverSourceSet: SourceSet = sourceSets.create("server")
serverSourceSet.java.srcDir("src/server/java")

configurations[serverSourceSet.implementationConfigurationName].extendsFrom(configurations["implementation"])
configurations[serverSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations["runtimeOnly"])

dependencies {
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:html"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion:sources")
    implementation("com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion:sources")

    gwtCompiler("org.gwtproject:gwt-dev:$gwtVersion")
    gwtCompiler("org.gwtproject:gwt-user:$gwtVersion")
}

val gwtModule = "com.github.czyzby.websocket.examples.GdxDefinition"
val gwtOutputDir = layout.buildDirectory.dir("dist/gwt")
val dependentSourceDirs = files(
    project(":examples:core").layout.projectDirectory.dir("src/main/java"),
    project(":libraries:core").layout.projectDirectory.dir("src/main/java"),
    project(":libraries:backends:html").layout.projectDirectory.dir("src/main/java"),
)
val gwtExecutionClasspath = files(
    sourceSets["main"].allSource.srcDirs,
    dependentSourceDirs,
    sourceSets["main"].runtimeClasspath,
    gwtCompiler,
)

tasks.register<Sync>("gwtPrepareWebApp") {
    from("src/main/webapp")
    into(gwtOutputDir)
}

val gwtSyncAssets by tasks.registering(Sync::class) {
    from(layout.projectDirectory.dir("war/assets"))
    into(gwtOutputDir.map { it.dir("assets") })
    onlyIf { layout.projectDirectory.dir("war/assets").asFile.exists() }
}

tasks.register<JavaExec>("gwtCompile") {
    group = "example-gwt"
    description = "Compile the websocket GWT example"
    dependsOn("classes", "gwtPrepareWebApp")
    mainClass.set("com.google.gwt.dev.Compiler")
    classpath = gwtExecutionClasspath
    jvmArgs("-Xmx1G")
    args(
        "-war", gwtOutputDir.get().asFile.absolutePath,
        "-strict",
        "-style", "PRETTY",
        "-optimize", "9",
        gwtModule,
    )
    finalizedBy(gwtSyncAssets)
}

tasks.register<JavaExec>("superDev") {
    group = "example-gwt"
    description = "Start the GWT Super Dev code server for the websocket example"
    dependsOn("classes", "gwtPrepareWebApp")
    mainClass.set("com.google.gwt.dev.codeserver.CodeServer")
    classpath = gwtExecutionClasspath
    jvmArgs("-Xmx1G")
    args(
        "-launcherDir", gwtOutputDir.get().asFile.absolutePath,
        gwtModule,
    )
}

tasks.register<JavaExec>("gwtRun") {
    group = "example-gwt"
    description = "Compile and serve the websocket GWT example"
    dependsOn("gwtCompile", serverSourceSet.classesTaskName)
    mainClass.set("com.github.czyzby.websocket.examples.gwt.GwtHttpServer")
    classpath = serverSourceSet.runtimeClasspath
    args(gwtOutputDir.get().asFile.absolutePath, "8080")
}
