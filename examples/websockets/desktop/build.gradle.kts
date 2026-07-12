plugins {
    application
}

val gdxVersion: String by rootProject.extra

application {
    mainClass.set("com.github.czyzby.websocket.examples.desktop.DesktopLauncher")
}

dependencies {
    implementation(project(":examples:websockets:core"))
    implementation(project(":libraries:backends:common"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}
