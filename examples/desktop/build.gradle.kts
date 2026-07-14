plugins {
    application
}

val gdxVersion: String by rootProject.extra

application {
    mainClass.set("com.github.czyzby.websocket.examples.desktop.DesktopLauncher")
}

tasks.withType<JavaExec>().configureEach {
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

dependencies {
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:common"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}
