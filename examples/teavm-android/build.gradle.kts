import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("com.android.application") version "8.10.1"
    id("com.github.xpenatan.gdx-teavm") version "-SNAPSHOT"
}

val gdxVersion: String by rootProject.extra

val generatedAndroidDir = layout.buildDirectory.dir("generated/gdx-teavm/android")
val generatedAndroidCMakeFile = generatedAndroidDir.map { it.file("CMakeLists.txt") }
val generatedWebSocketsAndroidCMakeFile = generatedAndroidDir.map {
    it.file("c/external_cpp/cmake/pre_target/gdx_websockets_android.cmake")
}
val generatedDefaultFontAssetsDir = layout.buildDirectory.dir("generated/gdx-default-font-assets")
val androidCxxDir = rootProject.layout.buildDirectory.dir("android-cxx/examples-teavm-android")
val gdxFontConfiguration = configurations.detachedConfiguration(
    dependencies.create("com.badlogicgames.gdx:gdx:$gdxVersion")
).apply {
    isTransitive = false
}

val extractGdxDefaultFontAssets = tasks.register<Copy>("extractGdxDefaultFontAssets") {
    from({ zipTree(gdxFontConfiguration.singleFile) }) {
        include("com/badlogic/gdx/utils/lsans-15.fnt")
        include("com/badlogic/gdx/utils/lsans-15.png")
    }
    into(generatedDefaultFontAssetsDir)
}

android {
    namespace = "com.github.czyzby.websocket.examples.teavm.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.czyzby.websocket.examples.teavm.android"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isJniDebuggable = true
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=Debug"
                }
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=Release"
                }
            }
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = generatedAndroidCMakeFile.get().asFile
            buildStagingDirectory = androidCxxDir.get().asFile
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(generatedDefaultFontAssetsDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":libraries:backends:teavm-android"))
    add("teavm", project(":examples:core"))
    add("teavm", project(":libraries:backends:teavm-android"))
}

gdxTeaVM {
    android {
        mainClass.set("WebSocketsAndroidLauncher")
        optimization.set(OptimizationLevel.NONE)
        debugInformation.set(false)
        obfuscated.set(false)
        minHeapSizeMb.set(16)
        maxHeapSizeMb.set(128)
    }
}

val patchGeneratedAndroidCMake = tasks.register("patchGeneratedAndroidCMake") {
    dependsOn("gdx_teavm_android_generate")

    doLast {
        val cmakeFile = generatedAndroidCMakeFile.get().asFile
        if (!cmakeFile.isFile) {
            return@doLast
        }

        val includeSnippet = """include("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/cmake/pre_target/gdx_websockets_android.cmake")"""
        val removeSnippet = """list(REMOVE_ITEM SOURCES "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/teavm_optimizations/teavm/teavm_spritebatch.c")"""
        val webSocketsCMakeFile = generatedWebSocketsAndroidCMakeFile.get().asFile

        if (!webSocketsCMakeFile.isFile) {
            webSocketsCMakeFile.parentFile.mkdirs()
            webSocketsCMakeFile.writeText(
                """
                include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")
                list(APPEND SOURCES "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets/teavm_websocket_android.c")
                """.trimIndent() + "\n"
            )
        }

        var text = cmakeFile.readLines()
            .filterNot { line ->
                line.contains(includeSnippet) || line.contains(removeSnippet)
            }
            .joinToString("\n")

        if (!text.contains(includeSnippet) || !text.contains(removeSnippet)) {
            text = text.replace(
                "add_library(app SHARED \${SOURCES})",
                includeSnippet + "\n" +
                        removeSnippet + "\n" +
                        "add_library(app SHARED \${SOURCES})"
            )
        }

        cmakeFile.writeText(text)
    }
}

tasks.named("preBuild").configure {
    dependsOn(extractGdxDefaultFontAssets)
}

tasks.matching {
    it.name.startsWith("configureCMake")
}.configureEach {
    dependsOn(patchGeneratedAndroidCMake)
}

tasks.matching {
    it.name.startsWith("externalNativeBuildClean")
}.configureEach {
    onlyIf("generated gdx-teavm Android CMake source exists") {
        generatedAndroidCMakeFile.get().asFile.isFile && androidCxxDir.get().asFile.isDirectory
    }
}

tasks.named("clean").configure {
    doFirst {
        project.delete(androidCxxDir)
        project.delete(layout.projectDirectory.dir(".cxx"))
    }
}
