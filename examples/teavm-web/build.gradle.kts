plugins {
    `java-library`
}

val gdxTeaVMVersion: String by rootProject.extra

dependencies {
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:teavm-web"))
    implementation("com.github.xpenatan.gdx-teavm:backend-shared:$gdxTeaVMVersion")
    implementation("com.github.xpenatan.gdx-teavm:backend-web:$gdxTeaVMVersion")
}

val buildMainClass = "com.github.czyzby.websocket.examples.teavmweb.BuildTeaVMWebExample"

tasks.register<JavaExec>("teavmWebJsBuild") {
    group = "example-web"
    description = "Generate the TeaVM JavaScript web example"
    mainClass.set(buildMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    args("js")
}

tasks.register<JavaExec>("teavmWebJsRun") {
    group = "example-web"
    description = "Generate and serve the TeaVM JavaScript web example"
    mainClass.set(buildMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    args("js", "serve")
}

tasks.register<JavaExec>("teavmWebWasmBuild") {
    group = "example-web"
    description = "Generate the TeaVM Wasm web example"
    mainClass.set(buildMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    args("wasm")
}

tasks.register<JavaExec>("teavmWebWasmRun") {
    group = "example-web"
    description = "Generate and serve the TeaVM Wasm web example"
    mainClass.set(buildMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    args("wasm", "serve")
}
