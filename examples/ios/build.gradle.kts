plugins {
    `java-library`
    id("com.mobidevelop.robovm") version "2.3.24"
}

val gdxVersion: String by rootProject.extra
val roboVMVersion: String by rootProject.extra
val iosTaskGroup = "example-ios"
val iosSimulatorName = providers.gradleProperty("iosSimulatorName")
val iosSimulatorSdk = providers.gradleProperty("iosSimulatorSdk")

if (iosSimulatorName.isPresent) {
    extensions.extraProperties.set("robovm.device.name", iosSimulatorName.get())
}
if (iosSimulatorSdk.isPresent) {
    extensions.extraProperties.set("robovm.sdk.version", iosSimulatorSdk.get())
}

val generatedDefaultFontAssetsDir = layout.buildDirectory.dir("generated/gdx-default-font-assets")
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

sourceSets {
    main {
        resources.srcDir(generatedDefaultFontAssetsDir)
    }
}

robovm {
    archs = "x86_64:arm64"
    isIosSkipSigning = true
}

dependencies {
    implementation(project(":examples:core"))
    implementation(project(":libraries:backends:common"))
    implementation("com.badlogicgames.gdx:gdx-backend-robovm-metalangle:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-ios")
    implementation("com.mobidevelop.robovm:robovm-rt:$roboVMVersion")
    implementation("com.mobidevelop.robovm:robovm-cocoatouch:$roboVMVersion")
}

tasks.named("processResources").configure {
    dependsOn(extractGdxDefaultFontAssets)
}

tasks.named("sourcesJar").configure {
    dependsOn(extractGdxDefaultFontAssets)
}

listOf(
    "launchIPhoneSimulator" to "Build and run the websocket example in the iPhone simulator",
    "launchIPadSimulator" to "Build and run the websocket example in the iPad simulator",
    "launchIOSDevice" to "Build and run the websocket example on a connected iOS device",
    "robovmInstall" to "Build the websocket example as an iOS app bundle",
    "createIPA" to "Build the websocket example as an IPA archive",
    "robovmArchive" to "Build the websocket example archive used by createIPA"
).forEach { (taskName, taskDescription) ->
    tasks.named(taskName).configure {
        group = iosTaskGroup
        description = taskDescription
    }
}

tasks.register("iosRunSimulator") {
    group = iosTaskGroup
    description = "Build and run the websocket example in the iPhone simulator"
    dependsOn("launchIPhoneSimulator")
}

tasks.register("iosRunIPadSimulator") {
    group = iosTaskGroup
    description = "Build and run the websocket example in the iPad simulator"
    dependsOn("launchIPadSimulator")
}

tasks.register("iosRunDevice") {
    group = iosTaskGroup
    description = "Build and run the websocket example on a connected iOS device"
    dependsOn("launchIOSDevice")
}

tasks.register("iosBuildApp") {
    group = iosTaskGroup
    description = "Build the websocket example as an iOS app bundle"
    dependsOn("robovmInstall")
}

tasks.register("iosPackageIpa") {
    group = iosTaskGroup
    description = "Build the websocket example as an IPA archive"
    dependsOn("createIPA")
}

tasks.register<Exec>("iosListSimulators") {
    group = iosTaskGroup
    description = "List available iOS simulators and runtimes"
    commandLine("xcrun", "simctl", "list", "devices", "available")
}
