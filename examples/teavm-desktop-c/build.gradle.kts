import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import java.io.ByteArrayOutputStream

plugins {
    `java-library`
}

val gdxVersion: String by rootProject.extra
val gdxTeaVMVersion: String by rootProject.extra

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation(project(":examples:core"))
    implementation("com.github.xpenatan.gdx-teavm:backend-glfw:$gdxTeaVMVersion")
    implementation(project(":libraries:backends:teavm-desktop-c"))
}

val buildMainClass = "com.github.czyzby.websocket.examples.teavmdesktopc.BuildTeaVMDesktopCExample"
val desktopCTaskGroup = "example-desktop-c"
val desktopCOutputDir = layout.buildDirectory.dir("dist")
val desktopCCMakeFile = desktopCOutputDir.map { it.file("CMakeLists.txt") }
val desktopCWebSocketsPreCMakeFile = desktopCOutputDir.map {
    it.file("c/external_cpp/cmake/pre_target/gdx_websockets_glfw.cmake")
}
val desktopCWebSocketsPostCMakeFile = desktopCOutputDir.map {
    it.file("c/external_cpp/cmake/post_target/gdx_websockets_glfw.cmake")
}
val desktopCReleaseDir = desktopCOutputDir.map { it.dir("c/release") }
val linuxCurlPath = providers.gradleProperty("gdxTeaVMLinuxCurlPath")
val macCurlPath = providers.gradleProperty("gdxTeaVMMacCurlPath")
val defaultLinuxCurlBuildScript = rootProject.layout.projectDirectory.file(
    "libraries/backends/teavm-desktop-c/scripts/build-linux-libcurl-wss.sh"
)
val defaultLinuxCurlRuntime = rootProject.layout.projectDirectory.file(
    "libraries/backends/teavm-desktop-c/scripts/build/libcurl-wss/install/lib/libcurl.so.4"
)
val defaultMacCurlBuildScript = rootProject.layout.projectDirectory.file(
    "libraries/backends/teavm-desktop-c/scripts/build-mac-libcurl-wss.sh"
)
val defaultMacCurlRuntime = rootProject.layout.projectDirectory.file(
    "libraries/backends/teavm-desktop-c/scripts/build/libcurl-wss-macos/install/lib/libcurl.4.dylib"
)
val resolvedLinuxCurlPathKey = "resolvedGdxTeaVMLinuxCurlPath"
val resolvedMacCurlPathKey = "resolvedGdxTeaVMMacCurlPath"

fun isLinuxHost(): Boolean = System.getProperty("os.name").lowercase().contains("linux")
fun isMacHost(): Boolean = System.getProperty("os.name").lowercase().contains("mac")

fun supportsSystemCurlWebSockets(): Boolean {
    val output = ByteArrayOutputStream()
    exec {
        isIgnoreExitValue = true
        commandLine("bash", "-lc", "curl-config --protocols 2>/dev/null || curl --version 2>/dev/null")
        standardOutput = output
        errorOutput = output
    }
    val text = output.toString()
    return Regex("\\bws\\b").containsMatchIn(text) && Regex("\\bwss\\b").containsMatchIn(text)
}

val prepareLinuxCurlRuntime = tasks.register("prepareLinuxCurlRuntime") {
    group = desktopCTaskGroup
    description = "Prepare a ws/wss-capable libcurl runtime for Linux desktop-c example runs"
    outputs.upToDateWhen { false }
    onlyIf { isLinuxHost() && linuxCurlPath.orNull.isNullOrBlank() }

    doLast {
        val bundledRuntime = defaultLinuxCurlRuntime.asFile
        if (bundledRuntime.isFile) {
            logger.lifecycle("Using bundled Linux libcurl runtime: ${bundledRuntime.absolutePath}")
            project.extensions.extraProperties[resolvedLinuxCurlPathKey] = bundledRuntime.absolutePath
            return@doLast
        }

        if (supportsSystemCurlWebSockets()) {
            logger.lifecycle("System libcurl already supports ws/wss; no bundled Linux runtime needed.")
            return@doLast
        }

        val buildScript = defaultLinuxCurlBuildScript.asFile
        if (!buildScript.isFile) {
            throw IllegalStateException("Missing Linux libcurl build script: $buildScript")
        }

        logger.lifecycle("System libcurl does not support ws/wss. Building a bundled Linux libcurl runtime...")
        exec {
            commandLine("bash", buildScript.absolutePath)
        }

        if (!bundledRuntime.isFile) {
            throw IllegalStateException("Expected bundled Linux libcurl runtime was not created: $bundledRuntime")
        }

        logger.lifecycle("Bundled Linux libcurl runtime ready: ${bundledRuntime.absolutePath}")
        project.extensions.extraProperties[resolvedLinuxCurlPathKey] = bundledRuntime.absolutePath
    }
}

val prepareMacCurlRuntime = tasks.register("prepareMacCurlRuntime") {
    group = desktopCTaskGroup
    description = "Prepare a ws/wss-capable libcurl runtime for macOS desktop-c example runs"
    outputs.upToDateWhen { false }
    onlyIf { isMacHost() && macCurlPath.orNull.isNullOrBlank() }

    doLast {
        val bundledRuntime = defaultMacCurlRuntime.asFile
        if (bundledRuntime.isFile) {
            logger.lifecycle("Using bundled macOS libcurl runtime: ${bundledRuntime.absolutePath}")
            project.extensions.extraProperties[resolvedMacCurlPathKey] = bundledRuntime.absolutePath
            return@doLast
        }

        if (supportsSystemCurlWebSockets()) {
            logger.lifecycle("System libcurl already supports ws/wss; no bundled macOS runtime needed.")
            return@doLast
        }

        val buildScript = defaultMacCurlBuildScript.asFile
        if (!buildScript.isFile) {
            throw IllegalStateException("Missing macOS libcurl build script: $buildScript")
        }

        logger.lifecycle("System libcurl does not support ws/wss. Building a bundled macOS libcurl runtime...")
        exec {
            commandLine("bash", buildScript.absolutePath)
        }

        if (!bundledRuntime.isFile) {
            throw IllegalStateException("Expected bundled macOS libcurl runtime was not created: $bundledRuntime")
        }

        logger.lifecycle("Bundled macOS libcurl runtime ready: ${bundledRuntime.absolutePath}")
        project.extensions.extraProperties[resolvedMacCurlPathKey] = bundledRuntime.absolutePath
    }
}

fun JavaExec.configureNativeCurlRuntime() {
    dependsOn(prepareLinuxCurlRuntime)
    dependsOn(prepareMacCurlRuntime)
    doFirst {
        val autoResolvedLinuxPath = if (project.extensions.extraProperties.has(resolvedLinuxCurlPathKey)) {
            project.extensions.extraProperties[resolvedLinuxCurlPathKey] as String
        } else {
            null
        }
        val autoResolvedMacPath = if (project.extensions.extraProperties.has(resolvedMacCurlPathKey)) {
            project.extensions.extraProperties[resolvedMacCurlPathKey] as String
        } else {
            null
        }
        val configuredLinuxPath = linuxCurlPath.orNull?.takeIf { it.isNotBlank() } ?: autoResolvedLinuxPath
        if (!configuredLinuxPath.isNullOrBlank()) {
            systemProperty("gdxTeaVMLinuxCurlPath", configuredLinuxPath)
        }

        val configuredMacPath = macCurlPath.orNull?.takeIf { it.isNotBlank() } ?: autoResolvedMacPath
        if (!configuredMacPath.isNullOrBlank()) {
            systemProperty("gdxTeaVMMacCurlPath", configuredMacPath)
        }
    }
}

tasks.register<JavaExec>("teavmDesktopCGenerate") {
    group = desktopCTaskGroup
    description = "Generate TeaVM C sources for the websocket GLFW example"
    mainClass.set(buildMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    configureNativeCurlRuntime()
    args("Debug")
}

val patchGeneratedDesktopCCMake = tasks.register("patchGeneratedDesktopCCMake") {
    dependsOn("teavmDesktopCGenerate")

    doLast {
        val cmakeFile = desktopCCMakeFile.get().asFile
        if (!cmakeFile.isFile) {
            throw IllegalStateException("Missing generated CMakeLists.txt: $cmakeFile")
        }

        var text = cmakeFile.readText()
        val preSnippet = """include("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/cmake/pre_target/gdx_websockets_glfw.cmake")"""
        val postSnippet = """set(TEAVM_APP_TARGET websockets)
include("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/cmake/post_target/gdx_websockets_glfw.cmake")"""
        val targetLine = "add_executable(websockets \${SOURCES})"
        val preCMakeFile = desktopCWebSocketsPreCMakeFile.get().asFile
        val postCMakeFile = desktopCWebSocketsPostCMakeFile.get().asFile

        if (!preCMakeFile.isFile) {
            preCMakeFile.parentFile.mkdirs()
            preCMakeFile.writeText(
                """
                if(WIN32 OR UNIX)
                  set(TEAVM_WEBSOCKETS_GLFW_SOURCE "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets/teavm_websocket_glfw.c")
                  if(EXISTS "${'$'}{TEAVM_WEBSOCKETS_GLFW_SOURCE}")
                    list(APPEND SOURCES "${'$'}{TEAVM_WEBSOCKETS_GLFW_SOURCE}")
                  endif()
                endif()
                """.trimIndent() + "\n"
            )
        }

        if (!postCMakeFile.isFile) {
            postCMakeFile.parentFile.mkdirs()
            postCMakeFile.writeText(
                """
                if(WIN32)
                  target_include_directories(${'$'}{TEAVM_APP_TARGET} PRIVATE
                      "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")
                  target_link_libraries(${'$'}{TEAVM_APP_TARGET} PRIVATE winhttp)
                endif()

                if(UNIX)
                  find_package(Threads REQUIRED)
                  target_include_directories(${'$'}{TEAVM_APP_TARGET} PRIVATE
                      "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")
                  target_link_libraries(${'$'}{TEAVM_APP_TARGET} PRIVATE Threads::Threads ${'$'}{CMAKE_DL_LIBS})
                endif()
                """.trimIndent() + "\n"
            )
        }

        if (!text.contains(preSnippet) || !text.contains(postSnippet)) {
            text = text.replace(
                targetLine,
                preSnippet + "\n" + targetLine + "\n" + postSnippet
            )
        }

        text = text.replace(
            "target_link_libraries(websockets glfw3 opengl32 glew32s)",
            "target_link_libraries(websockets PRIVATE glfw3 opengl32 glew32s)"
        )
        text = text.replace(
            "target_link_libraries(websockets gdx2d_freetype_bridge)",
            "target_link_libraries(websockets PRIVATE gdx2d_freetype_bridge)"
        )
        text = text.replace(
            "target_link_libraries(websockets PRIVATE OpenGL::GL glfw GLEW::GLEW m)",
            "target_link_libraries(websockets PRIVATE OpenGL::GL glfw GLEW::GLEW m)"
        )
        cmakeFile.writeText(text)
    }
}

fun Exec.configureDesktopCScript(scriptName: String) {
    dependsOn(patchGeneratedDesktopCCMake)
    workingDir = desktopCOutputDir.get().asFile
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", scriptName)
    } else {
        commandLine("bash", scriptName.replace(".bat", ".sh"))
    }
}

fun desktopCExecutable(buildType: String): String {
    val extension = if (System.getProperty("os.name").lowercase().contains("windows")) ".exe" else ""
    return desktopCReleaseDir.get().file("websockets_${buildType.lowercase()}$extension").asFile.absolutePath
}

val teavmDesktopCDebugBuild = tasks.register<Exec>("teavmDesktopCDebugBuild") {
    group = desktopCTaskGroup
    description = "Generate TeaVM C sources and build the Debug websocket GLFW executable"
    configureDesktopCScript("app_debug.bat")
}

tasks.register<Exec>("teavmDesktopCReleaseBuild") {
    group = desktopCTaskGroup
    description = "Generate TeaVM C sources and build the Release websocket GLFW executable"
    configureDesktopCScript("app_release.bat")
}

tasks.register("teavmDesktopCBuild") {
    group = desktopCTaskGroup
    description = "Compatibility alias for teavmDesktopCDebugBuild"
    dependsOn(teavmDesktopCDebugBuild)
}

tasks.register<Exec>("teavmDesktopCDebugRun") {
    group = desktopCTaskGroup
    description = "Generate, build, and run the Debug websocket GLFW executable with native console log output"
    dependsOn(teavmDesktopCDebugBuild)
    workingDir = desktopCReleaseDir.get().asFile
    commandLine(desktopCExecutable("debug"))
}

tasks.register<Exec>("teavmDesktopCReleaseRun") {
    group = desktopCTaskGroup
    description = "Generate, build, and run the Release websocket GLFW executable"
    dependsOn("teavmDesktopCReleaseBuild")
    workingDir = desktopCReleaseDir.get().asFile
    commandLine(desktopCExecutable("release"))
}

tasks.register<Exec>("teavmDesktopCReleaseConsoleRun") {
    group = desktopCTaskGroup
    description = "Generate, build, and run the Release websocket GLFW executable with native console log output"
    dependsOn("teavmDesktopCReleaseBuild")
    workingDir = desktopCReleaseDir.get().asFile
    commandLine(desktopCExecutable("release"))
}
