plugins {
    id("com.android.application") version "8.10.1"
}

val gdxVersion: String by rootProject.extra

val generatedDefaultFontAssetsDir = layout.buildDirectory.dir("generated/gdx-default-font-assets")
val generatedJniLibsDir = layout.buildDirectory.dir("generated/android-natives")
val gdxFontConfiguration = configurations.detachedConfiguration(
    dependencies.create("com.badlogicgames.gdx:gdx:$gdxVersion")
).apply {
    isTransitive = false
}
val natives by configurations.creating

val extractGdxDefaultFontAssets = tasks.register<Copy>("extractGdxDefaultFontAssets") {
    from({ zipTree(gdxFontConfiguration.singleFile) }) {
        include("com/badlogic/gdx/utils/lsans-15.fnt")
        include("com/badlogic/gdx/utils/lsans-15.png")
    }
    into(generatedDefaultFontAssetsDir)
}

android {
    namespace = "com.github.czyzby.websocket.examples.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.czyzby.websocket.examples.android"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(generatedDefaultFontAssetsDir)
            jniLibs.srcDir(generatedJniLibsDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:common"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val copyAndroidNatives = tasks.register("copyAndroidNatives") {
    doFirst {
        delete(generatedJniLibsDir)
    }

    doLast {
        natives.files.forEach { jar ->
            val outputDir = when {
                jar.name.endsWith("natives-armeabi-v7a.jar") -> generatedJniLibsDir.get().dir("armeabi-v7a").asFile
                jar.name.endsWith("natives-arm64-v8a.jar") -> generatedJniLibsDir.get().dir("arm64-v8a").asFile
                jar.name.endsWith("natives-x86.jar") -> generatedJniLibsDir.get().dir("x86").asFile
                jar.name.endsWith("natives-x86_64.jar") -> generatedJniLibsDir.get().dir("x86_64").asFile
                else -> null
            }

            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(extractGdxDefaultFontAssets, copyAndroidNatives)
}
