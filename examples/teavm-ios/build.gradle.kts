import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("com.github.xpenatan.gdx-teavm") version "-SNAPSHOT"
}

val gdxVersion: String by rootProject.extra
val iosTaskGroup = "example-teavm-ios"
val iosXcodeProjectName = "WebSocketsTeaVMIOS"
val iosBundleIdentifier = "com.github.czyzby.websocket.examples.teavm.ios"
val iosSimulatorUdid = providers.gradleProperty("iosSimulatorUdid")
val iosSimulatorName = providers.gradleProperty("iosSimulatorName")
val iosDeviceUdid = providers.gradleProperty("iosDeviceUdid")
val iosSkipSigning = providers.gradleProperty("iosSkipSigning").map(String::toBoolean).getOrElse(true)
val iosDevelopmentTeam = providers.gradleProperty("iosDevelopmentTeam")
val iosSignIdentity = providers.gradleProperty("iosSignIdentity")
val iosProvisioningProfile = providers.gradleProperty("iosProvisioningProfile")
val iosNativeGeneratingAliases = setOf(
    "iosGenerate",
    "iosInitXcode",
    "iosRegenerateXcode",
    "iosOpenXcode",
    "iosBuildSimulator",
    "iosTerminateSimulatorApp",
    "iosUninstallSimulatorApp",
    "iosInstallSimulator",
    "iosLaunchSimulator",
    "iosRunSimulator",
    "iosBuildDevice",
    "iosBuildApp",
    "iosInstallDeviceDevicectl",
    "iosLaunchDeviceDevicectl",
    "iosRunDeviceDevicectl",
    "iosRunDevice",
    "iosStageDeviceAppDevicectl",
    "iosStageIPAPayload",
    "iosPackageIpa"
)
val requestedTaskNames = gradle.startParameter.taskNames
val requestedTeaVMIOSAlias = requestedTaskNames.any { it.substringAfterLast(":") in iosNativeGeneratingAliases }
val requestedTeaVMIOSPluginTask = requestedTaskNames.any { it.substringAfterLast(":").startsWith("gdx_teavm_ios") }
if (requestedTeaVMIOSAlias && !requestedTeaVMIOSPluginTask) {
    gradle.startParameter.setTaskNames(requestedTaskNames + "${project.path}:gdx_teavm_ios_generate")
}
val generatedDefaultFontAssetsDir = layout.buildDirectory.dir("generated/gdx-default-font-assets")
val generatedIOSDir = layout.buildDirectory.dir("dist/ios")
val generatedIOSCMakeFile = generatedIOSDir.map { it.file("CMakeLists.txt") }
val generatedIOSAppIncludeFile = generatedIOSDir.map { it.file("c/src/app_include.c") }
val generatedIOSBridgeFile = generatedIOSDir.map { it.file("c/src/ios_bridge.c") }
val generatedIOSExternalCppDir = generatedIOSDir.map { it.dir("c/external_cpp") }
val generatedIOSXcodeProjectFile = generatedIOSDir.map {
    it.file("xcode/$iosXcodeProjectName.xcodeproj/project.pbxproj")
}
val generatedIOSXcodeInfoPlistFile = generatedIOSDir.map {
    it.file("xcode/Sources/Info.plist")
}
val generatedIOSXcodeProjectDir = generatedIOSDir.map {
    it.dir("xcode/$iosXcodeProjectName.xcodeproj")
}
val iosDerivedDataDir = layout.buildDirectory.dir("xcode-derived/ios")
val iosSimulatorDebugAppBundle = iosDerivedDataDir.map {
    it.dir("Build/Products/Debug-iphonesimulator/$iosXcodeProjectName.app")
}
val iosDeviceDebugAppBundle = iosDerivedDataDir.map {
    it.dir("Build/Products/Debug-iphoneos/$iosXcodeProjectName.app")
}
val iosIPAPayloadDir = layout.buildDirectory.dir("ipa/Payload")
val iosDevicectlAppBundle = providers.provider {
    file("${System.getProperty("java.io.tmpdir")}/gdx-websockets-teavm-ios-devicectl/$iosXcodeProjectName.app")
}
val localIOSWebSocketExternalCppDir = project(":libraries:backends:teavm-ios")
    .layout.projectDirectory.dir("src/main/resources/external_cpp")
val localIOSXcodeSourcesTemplateDir = layout.projectDirectory.dir("src/main/resources/templates/ios/xcode/Sources")
val gdxFontConfiguration = configurations.detachedConfiguration(
    dependencies.create("com.badlogicgames.gdx:gdx:$gdxVersion")
).apply {
    isTransitive = false
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

val extractGdxDefaultFontAssets = tasks.register<Copy>("extractGdxDefaultFontAssets") {
    from({ zipTree(gdxFontConfiguration.singleFile) }) {
        include("com/badlogic/gdx/utils/lsans-15.fnt")
        include("com/badlogic/gdx/utils/lsans-15.png")
    }
    into(generatedDefaultFontAssetsDir)
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:teavm-ios"))
}

gdxTeaVM {
    assets.from(generatedDefaultFontAssetsDir)

    ios {
        mainClass.set("WebSocketsIOSLauncher")
        targetFileName.set("websockets")
        xcodeProjectName.set(iosXcodeProjectName)
        xcodeScheme.set(iosXcodeProjectName)
        bundleIdentifier.set(iosBundleIdentifier)
        optimization.set(OptimizationLevel.NONE)
        debugInformation.set(false)
        obfuscated.set(false)
        minHeapSizeMb.set(16)
        maxHeapSizeMb.set(128)
    }
}

val patchGeneratedIOSNativeProject = tasks.register("patchGeneratedIOSNativeProject") {
    group = iosTaskGroup
    description = "Patch generated TeaVM iOS native and Xcode files for the local websocket bridge"
    mustRunAfter("gdx_teavm_ios_generate", "gdx_teavm_ios_init_xcode", "gdx_teavm_ios_regenerate_xcode")

    doLast {
        copy {
            from(localIOSWebSocketExternalCppDir)
            into(generatedIOSExternalCppDir)
        }

        val infoPlistTemplateFile = localIOSXcodeSourcesTemplateDir.file("Info.plist").asFile
        val infoPlistFile = generatedIOSXcodeInfoPlistFile.get().asFile
        if (infoPlistTemplateFile.isFile && infoPlistFile.isFile) {
            infoPlistFile.writeText(infoPlistTemplateFile.readText())
        }

        val appIncludeFile = generatedIOSAppIncludeFile.get().asFile
        if (appIncludeFile.isFile) {
            val webSocketInclude = "#include \"../external_cpp/app_include/ios/teavm_websocket_ios.m\""
            val spriteBatchInclude = "#include \"../external_cpp/teavm_optimizations/teavm/teavm_spritebatch.c\""
            var appIncludeText = appIncludeFile.readText()
            if (appIncludeText.contains(webSocketInclude)) {
                appIncludeText = appIncludeText
                    .replace(webSocketInclude + "\n\n", "")
                    .replace(webSocketInclude + "\n", "")
                    .replace(webSocketInclude, "")
            }
            if (appIncludeText.contains(spriteBatchInclude)) {
                appIncludeText = appIncludeText
                    .replace(spriteBatchInclude + "\n", "")
                    .replace(spriteBatchInclude, "")
            }
            if (appIncludeText != appIncludeFile.readText()) {
                appIncludeFile.writeText(appIncludeText)
            }
        }

        val iosBridgeFile = generatedIOSBridgeFile.get().asFile
        if (iosBridgeFile.isFile) {
            var bridgeText = iosBridgeFile.readText()
            if (!bridgeText.contains("#include <stdlib.h>")) {
                bridgeText = bridgeText.replace(
                    "#include <stddef.h>\n",
                    "#include <stddef.h>\n#include <stdlib.h>\n#include <string.h>\n"
                )
            }
            val chdirToAssetsSnippet = """
                #if !defined(_WIN32)
                    if(workingDirectory != NULL && workingDirectory[0] != '\0') {
                        chdir(workingDirectory);
                    }
                #else
                    (void) workingDirectory;
                #endif
            """.trimIndent()
            val chdirToAppRootSnippet = """
                #if !defined(_WIN32)
                    if(workingDirectory != NULL && workingDirectory[0] != '\0') {
                        char* processDirectory = strdup(workingDirectory);
                        if(processDirectory != NULL) {
                            char* lastSeparator = strrchr(processDirectory, '/');
                            if(lastSeparator != NULL && strcmp(lastSeparator + 1, "assets") == 0) {
                                *lastSeparator = '\0';
                                chdir(processDirectory);
                            }
                            else {
                                chdir(workingDirectory);
                            }
                            free(processDirectory);
                        }
                        else {
                            chdir(workingDirectory);
                        }
                    }
                #else
                    (void) workingDirectory;
                #endif
            """.trimIndent()
            bridgeText = bridgeText.replace(chdirToAssetsSnippet, chdirToAppRootSnippet)
            iosBridgeFile.writeText(bridgeText)
        }

        val cmakeFile = generatedIOSCMakeFile.get().asFile
        if (cmakeFile.isFile) {
            var cmakeText = cmakeFile.readText()
            cmakeText = cmakeText.replace(
                "project(websockets C CXX)",
                "project(websockets C CXX OBJC)"
            )
            if (!cmakeText.contains("""include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp")""")) {
                cmakeText = cmakeText.replace(
                    """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/src")""",
                    """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/src")""" + "\n" +
                            """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp")"""
                )
            }
            if (!cmakeText.contains("""include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")""")) {
                cmakeText = cmakeText.replace(
                    """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp")""",
                    """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp")""" + "\n" +
                            """include_directories("${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")"""
                )
            }

            val objcSourceSnippet = """
                set(TEAVM_APP_INCLUDE_SOURCE "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/src/app_include.c")
                set_source_files_properties("${'$'}{TEAVM_APP_INCLUDE_SOURCE}" PROPERTIES
                    LANGUAGE OBJC
                    COMPILE_FLAGS "-fobjc-arc")
                set(SOURCES "${'$'}{TEAVM_APP_INCLUDE_SOURCE}")
            """.trimIndent()
            cmakeText = cmakeText.replace(
                objcSourceSnippet,
                """set(SOURCES "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/src/app_include.c")"""
            )
            val oldWebSocketObjcSourceSnippet = """
                set(TEAVM_WEBSOCKET_IOS_SOURCE "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/app_include/ios/teavm_websocket_ios.m")
                if(EXISTS "${'$'}{TEAVM_WEBSOCKET_IOS_SOURCE}")
                  list(APPEND SOURCES "${'$'}{TEAVM_WEBSOCKET_IOS_SOURCE}")
                  set_source_files_properties("${'$'}{TEAVM_WEBSOCKET_IOS_SOURCE}" PROPERTIES
                      LANGUAGE OBJC
                      COMPILE_FLAGS "-fobjc-arc")
                endif()

                add_library(websockets STATIC ${'$'}{SOURCES})
            """.trimIndent()
            cmakeText = cmakeText.replace(
                oldWebSocketObjcSourceSnippet,
                "add_library(websockets STATIC \${SOURCES})"
            )
            val removeSpriteBatchSnippet =
                """list(REMOVE_ITEM SOURCES "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/teavm_optimizations/teavm/teavm_spritebatch.c")"""

            val preTargetSnippet = """
                set(TEAVM_EXTENSION_PRE_TARGET_DIR "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/cmake/pre_target")
                if(EXISTS "${'$'}{TEAVM_EXTENSION_PRE_TARGET_DIR}")
                  file(GLOB TEAVM_EXTENSION_PRE_TARGET_FILES "${'$'}{TEAVM_EXTENSION_PRE_TARGET_DIR}/*.cmake")
                  list(SORT TEAVM_EXTENSION_PRE_TARGET_FILES)
                  foreach(TEAVM_EXTENSION_PRE_TARGET_FILE IN LISTS TEAVM_EXTENSION_PRE_TARGET_FILES)
                    include("${'$'}{TEAVM_EXTENSION_PRE_TARGET_FILE}")
                  endforeach()
                endif()
            """.trimIndent()
            if (!cmakeText.contains("TEAVM_EXTENSION_PRE_TARGET_DIR")) {
                cmakeText = cmakeText.replace(
                    "add_library(websockets STATIC \${SOURCES})",
                    preTargetSnippet + "\n\nadd_library(websockets STATIC \${SOURCES})"
                )
            }
            if (!cmakeText.contains(removeSpriteBatchSnippet)) {
                cmakeText = cmakeText.replace(
                    "add_library(websockets STATIC \${SOURCES})",
                    removeSpriteBatchSnippet + "\nadd_library(websockets STATIC \${SOURCES})"
                )
            }

            val postTargetSnippet = """
                set(TEAVM_EXTENSION_POST_TARGET_DIR "${'$'}{CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/cmake/post_target")
                if(EXISTS "${'$'}{TEAVM_EXTENSION_POST_TARGET_DIR}")
                  file(GLOB TEAVM_EXTENSION_POST_TARGET_FILES "${'$'}{TEAVM_EXTENSION_POST_TARGET_DIR}/*.cmake")
                  list(SORT TEAVM_EXTENSION_POST_TARGET_FILES)
                  foreach(TEAVM_EXTENSION_POST_TARGET_FILE IN LISTS TEAVM_EXTENSION_POST_TARGET_FILES)
                    include("${'$'}{TEAVM_EXTENSION_POST_TARGET_FILE}")
                  endforeach()
                endif()
            """.trimIndent()
            if (!cmakeText.contains("TEAVM_EXTENSION_POST_TARGET_DIR")) {
                cmakeText = cmakeText.trimEnd() + "\n\n" + postTargetSnippet + "\n"
            }

            cmakeFile.writeText(cmakeText)
        }

        val xcodeProjectFile = generatedIOSXcodeProjectFile.get().asFile
        if (!xcodeProjectFile.isFile) {
            return@doLast
        }

        val webSocketObjcBuildFileId = "1E2A57530000000000000004"
        val webSocketObjcFileRefId = "1E2A57530000000000000015"
        var xcodeText = xcodeProjectFile.readText()
        if (!xcodeText.contains("teavm_websocket_ios.m in Sources")) {
            xcodeText = xcodeText.replace(
                "\t\t1E2A00000000000000000003 /* app_include.c in Sources */ = {isa = PBXBuildFile; fileRef = 1E2A00000000000000000014 /* app_include.c */; };",
                "\t\t1E2A00000000000000000003 /* app_include.c in Sources */ = {isa = PBXBuildFile; fileRef = 1E2A00000000000000000014 /* app_include.c */; };\n" +
                        "\t\t$webSocketObjcBuildFileId /* teavm_websocket_ios.m in Sources */ = {isa = PBXBuildFile; fileRef = $webSocketObjcFileRefId /* teavm_websocket_ios.m */; settings = {COMPILER_FLAGS = \"-fobjc-arc\"; }; };"
            )
            xcodeText = xcodeText.replace(
                "\t\t1E2A00000000000000000014 /* app_include.c */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.c.c; name = app_include.c; path = \"../c/src/app_include.c\"; sourceTree = SOURCE_ROOT; };",
                "\t\t1E2A00000000000000000014 /* app_include.c */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.c.c; name = app_include.c; path = \"../c/src/app_include.c\"; sourceTree = SOURCE_ROOT; };\n" +
                        "\t\t$webSocketObjcFileRefId /* teavm_websocket_ios.m */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.c.objc; name = teavm_websocket_ios.m; path = \"../c/external_cpp/app_include/ios/teavm_websocket_ios.m\"; sourceTree = SOURCE_ROOT; };"
            )
            xcodeText = xcodeText.replace(
                "\t\t\t\t1E2A00000000000000000014 /* app_include.c */,",
                "\t\t\t\t1E2A00000000000000000014 /* app_include.c */,\n" +
                        "\t\t\t\t$webSocketObjcFileRefId /* teavm_websocket_ios.m */,"
            )
            xcodeText = xcodeText.replace(
                "\t\t\t\t1E2A00000000000000000003 /* app_include.c in Sources */,",
                "\t\t\t\t1E2A00000000000000000003 /* app_include.c in Sources */,\n" +
                        "\t\t\t\t$webSocketObjcBuildFileId /* teavm_websocket_ios.m in Sources */,"
            )
        }

        if (!xcodeText.contains(""""${'$'}(PROJECT_DIR)/../c/external_cpp/websockets"""")) {
            xcodeText = xcodeText.replace(
                """"${'$'}(PROJECT_DIR)/../c/external_cpp/teavm_stats",""",
                """"${'$'}(PROJECT_DIR)/../c/external_cpp/teavm_stats",""" + "\n" +
                        "\t\t\t\t\t\"${'$'}(PROJECT_DIR)/../c/external_cpp/websockets\","
            )
        }

        xcodeText = xcodeText.replace(
            "PRODUCT_BUNDLE_IDENTIFIER = com.github.xpenatan.gdxteavm.ios.spike;",
            "PRODUCT_BUNDLE_IDENTIFIER = $iosBundleIdentifier;"
        )

        xcodeProjectFile.writeText(xcodeText)
    }
}

val iosSyncXcodeProvisioningProfiles = tasks.register<Copy>("iosSyncXcodeProvisioningProfiles") {
    group = iosTaskGroup
    description = "Copy Xcode-managed provisioning profiles to the legacy iOS profile directory"

    val userHome = System.getProperty("user.home")
    val xcodeProfilesDir = file("$userHome/Library/Developer/Xcode/UserData/Provisioning Profiles")
    val mobileDeviceProfilesDir = file("$userHome/Library/MobileDevice/Provisioning Profiles")

    from(xcodeProfilesDir) {
        include("*.mobileprovision")
    }
    into(mobileDeviceProfilesDir)
    onlyIf {
        xcodeProfilesDir.isDirectory
    }
}

fun teavmIOSXcodeBuildArguments(sdk: String, configuration: String, destination: String?): List<String> {
    val arguments = mutableListOf(
        "-quiet",
        "-project",
        generatedIOSXcodeProjectDir.get().asFile.absolutePath,
        "-scheme",
        iosXcodeProjectName,
        "-sdk",
        sdk,
        "-configuration",
        configuration,
        "-derivedDataPath",
        iosDerivedDataDir.get().asFile.absolutePath
    )

    if (!destination.isNullOrBlank()) {
        arguments += listOf("-destination", destination)
    }

    if (iosSkipSigning) {
        arguments += "CODE_SIGNING_ALLOWED=NO"
    } else {
        arguments += "CODE_SIGNING_ALLOWED=YES"
        iosDevelopmentTeam.orNull?.takeIf(String::isNotBlank)?.let {
            arguments += "DEVELOPMENT_TEAM=$it"
        }
        iosSignIdentity.orNull?.takeIf(String::isNotBlank)?.let {
            arguments += "CODE_SIGN_IDENTITY=$it"
        }
        iosProvisioningProfile.orNull?.takeIf(String::isNotBlank)?.let {
            arguments += "PROVISIONING_PROFILE_SPECIFIER=$it"
        }
    }

    arguments += "build"
    return arguments
}

val iosSimulatorTarget = providers.provider {
    iosSimulatorUdid.orNull?.takeIf(String::isNotBlank)
        ?: iosSimulatorName.orNull?.takeIf(String::isNotBlank)
        ?: "booted"
}

fun Exec.configureTeaVMGeneratedXcodeBuild() {
    dependsOn(
        "gdx_teavm_ios_generate",
        "gdx_teavm_ios_init_xcode",
        patchGeneratedIOSNativeProject
    )
    executable = "xcodebuild"
}

tasks.register("iosGenerate") {
    group = iosTaskGroup
    description = "Generate TeaVM iOS native C sources and assets"
    dependsOn("gdx_teavm_ios_generate")
}

tasks.register("iosPrepareAngle") {
    group = iosTaskGroup
    description = "Download and extract the MetalANGLEKit frameworks used by the TeaVM iOS app"
    dependsOn("gdx_teavm_ios_prepare_angle")
}

tasks.register("iosInitXcode") {
    group = iosTaskGroup
    description = "Create the generated TeaVM iOS Xcode project if missing"
    dependsOn("gdx_teavm_ios_init_xcode")
}

tasks.register("iosRegenerateXcode") {
    group = iosTaskGroup
    description = "Regenerate the TeaVM iOS Xcode project from the template"
    dependsOn("gdx_teavm_ios_regenerate_xcode")
}

tasks.register("iosOpenXcode") {
    group = iosTaskGroup
    description = "Create and open the generated TeaVM iOS Xcode project"
    dependsOn("gdx_teavm_ios_open_xcode")
}

tasks.register("iosBuildSimulator") {
    group = iosTaskGroup
    description = "Generate and build the TeaVM iOS app for an iOS simulator"
    dependsOn("gdx_teavm_ios_build_simulator")
}

tasks.register<Exec>("iosTerminateSimulatorApp") {
    group = iosTaskGroup
    description = "Terminate the TeaVM iOS app on the selected simulator if it is running"
    dependsOn("iosBuildSimulator")
    executable = "xcrun"
    isIgnoreExitValue = true
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            return listOf("simctl", "terminate", iosSimulatorTarget.get(), iosBundleIdentifier)
        }
    })
}

tasks.register<Exec>("iosUninstallSimulatorApp") {
    group = iosTaskGroup
    description = "Uninstall the TeaVM iOS app from the selected simulator if it is already installed"
    dependsOn("iosTerminateSimulatorApp")
    executable = "xcrun"
    isIgnoreExitValue = true
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            return listOf("simctl", "uninstall", iosSimulatorTarget.get(), iosBundleIdentifier)
        }
    })
}

tasks.register<Exec>("iosInstallSimulator") {
    group = iosTaskGroup
    description = "Install the TeaVM iOS app on the selected simulator"
    dependsOn("iosUninstallSimulatorApp")
    executable = "xcrun"
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            return listOf(
                "simctl",
                "install",
                iosSimulatorTarget.get(),
                iosSimulatorDebugAppBundle.get().asFile.absolutePath
            )
        }
    })
}

tasks.register<Exec>("iosLaunchSimulator") {
    group = iosTaskGroup
    description = "Launch the TeaVM iOS app on the selected simulator"
    dependsOn("iosInstallSimulator")
    executable = "xcrun"
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            return listOf(
                "simctl",
                "launch",
                "--terminate-running-process",
                iosSimulatorTarget.get(),
                iosBundleIdentifier
            )
        }
    })
}

tasks.register("iosRunSimulator") {
    group = iosTaskGroup
    description = "Generate, build, install, and launch the TeaVM iOS app on a simulator"
    dependsOn("iosLaunchSimulator")
}

tasks.register<Exec>("iosBuildDevice") {
    group = iosTaskGroup
    description = "Generate and build the TeaVM iOS app for a connected iOS device"
    dependsOn(iosSyncXcodeProvisioningProfiles)
    configureTeaVMGeneratedXcodeBuild()
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val destination = iosDeviceUdid.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { "id=$it" }
                ?: "generic/platform=iOS"
            return teavmIOSXcodeBuildArguments("iphoneos", "Debug", destination)
        }
    })
}

tasks.register("iosBuildApp") {
    group = iosTaskGroup
    description = "Build the TeaVM iOS app bundle for a connected iOS device"
    dependsOn("iosBuildDevice")
}

val iosStageDeviceAppDevicectl = tasks.register<Sync>("iosStageDeviceAppDevicectl") {
    group = iosTaskGroup
    description = "Stage the signed TeaVM iOS app bundle in a system temporary directory for devicectl"
    dependsOn("iosBuildApp")
    from(iosDeviceDebugAppBundle)
    into(iosDevicectlAppBundle)
}

tasks.register<Exec>("iosInstallDeviceDevicectl") {
    group = iosTaskGroup
    description = "Install the signed TeaVM iOS app bundle on a connected device with Xcode devicectl"
    dependsOn(iosStageDeviceAppDevicectl)
    executable = "xcrun"
    doFirst {
        if (iosSkipSigning) {
            throw GradleException("Pass -PiosSkipSigning=false to install on a connected device.")
        }
    }
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val deviceUdid = iosDeviceUdid.orNull ?: throw GradleException(
                "Pass -PiosDeviceUdid=<device udid>. Use :examples:teavm-ios:iosListDevices to list connected devices."
            )
            return listOf(
                "devicectl",
                "device",
                "install",
                "app",
                "--device",
                deviceUdid,
                iosDevicectlAppBundle.get().absolutePath
            )
        }
    })
}

tasks.register<Exec>("iosLaunchDeviceDevicectl") {
    group = iosTaskGroup
    description = "Launch the installed TeaVM iOS app on a connected device with Xcode devicectl"
    dependsOn("iosInstallDeviceDevicectl")
    executable = "xcrun"
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val deviceUdid = iosDeviceUdid.orNull ?: throw GradleException(
                "Pass -PiosDeviceUdid=<device udid>. Use :examples:teavm-ios:iosListDevices to list connected devices."
            )
            return listOf(
                "devicectl",
                "device",
                "process",
                "launch",
                "--device",
                deviceUdid,
                "--terminate-existing",
                iosBundleIdentifier
            )
        }
    })
}

tasks.register("iosRunDeviceDevicectl") {
    group = iosTaskGroup
    description = "Build, install, and launch the TeaVM iOS app on a connected device with Xcode devicectl"
    dependsOn("iosLaunchDeviceDevicectl")
}

tasks.register("iosRunDevice") {
    group = iosTaskGroup
    description = "Build, install, and launch the TeaVM iOS app on a connected device"
    dependsOn("iosRunDeviceDevicectl")
}

val iosStageIPAPayload = tasks.register<Sync>("iosStageIPAPayload") {
    group = iosTaskGroup
    description = "Stage the TeaVM iOS app bundle in a Payload directory for IPA packaging"
    dependsOn("iosBuildApp")
    into(iosIPAPayloadDir)
    from(iosDeviceDebugAppBundle) {
        into("$iosXcodeProjectName.app")
    }
}

tasks.register<Zip>("iosPackageIpa") {
    group = iosTaskGroup
    description = "Build the TeaVM iOS app and package it as an IPA archive"
    dependsOn(iosStageIPAPayload)
    archiveFileName.set("websockets-teavm-ios.ipa")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(iosIPAPayloadDir.map { it.asFile.parentFile }) {
        include("Payload/**")
    }
}

tasks.register<Exec>("iosListSimulators") {
    group = iosTaskGroup
    description = "List available iOS simulators and runtimes"
    commandLine("xcrun", "simctl", "list", "devices", "available")
}

tasks.register<Exec>("iosListDevices") {
    group = iosTaskGroup
    description = "List connected iOS devices and available simulators"
    commandLine("xcrun", "xctrace", "list", "devices")
}

tasks.matching {
    it.name.startsWith("gdx_teavm_ios")
}.configureEach {
    group = iosTaskGroup
    dependsOn(extractGdxDefaultFontAssets)
}

afterEvaluate {
    tasks.matching {
        it.name.startsWith("gdx_teavm_ios")
    }.configureEach {
        group = iosTaskGroup
        dependsOn(extractGdxDefaultFontAssets)
    }

    tasks.named("gdx_teavm_ios_generate").configure {
        finalizedBy(patchGeneratedIOSNativeProject)
    }

    tasks.matching {
        it.name == "gdx_teavm_ios_init_xcode" || it.name == "gdx_teavm_ios_regenerate_xcode"
    }.configureEach {
        finalizedBy(patchGeneratedIOSNativeProject)
    }

    tasks.named("gdx_teavm_ios_open_xcode").configure {
        dependsOn(patchGeneratedIOSNativeProject)
    }

    tasks.matching {
        it.name == "gdx_teavm_ios_build_simulator" || it.name == "gdx_teavm_ios_run_simulator"
    }.configureEach {
        dependsOn(patchGeneratedIOSNativeProject)
    }
}
