plugins {
    `java-library`
    id("com.mobidevelop.robovm") version "2.3.24"
}

val gdxVersion: String by rootProject.extra
val roboVMVersion: String by rootProject.extra
val iosTaskGroup = "example-ios"
val iosSimulatorName = providers.gradleProperty("iosSimulatorName")
val iosSimulatorSdk = providers.gradleProperty("iosSimulatorSdk")
val iosDeviceUdid = providers.gradleProperty("iosDeviceUdid")
val iosSkipSigning = providers.gradleProperty("iosSkipSigning").map(String::toBoolean).getOrElse(true)
val iosSignIdentityProperty = providers.gradleProperty("iosSignIdentity")
val iosProvisioningProfileProperty = providers.gradleProperty("iosProvisioningProfile")
val iosBundleIdentifier = "com.github.czyzby.websocket.examples.ios"
val iosAppBundle = layout.buildDirectory.file("robovm.tmp/gdx-websockets-ios.app")
val iosDevicectlAppBundle = providers.provider {
    file("${System.getProperty("java.io.tmpdir")}/gdx-websockets-ios-devicectl/gdx-websockets-ios.app")
}

if (iosSimulatorName.isPresent) {
    extensions.extraProperties.set("robovm.device.name", iosSimulatorName.get())
}
if (iosSimulatorSdk.isPresent) {
    extensions.extraProperties.set("robovm.sdk.version", iosSimulatorSdk.get())
}
if (iosDeviceUdid.isPresent) {
    extensions.extraProperties.set("robovm.device.udid", iosDeviceUdid.get())
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

val iosSyncXcodeProvisioningProfiles = tasks.register<Copy>("iosSyncXcodeProvisioningProfiles") {
    group = iosTaskGroup
    description = "Copy Xcode-managed provisioning profiles to the legacy RoboVM profile directory"

    val userHome = System.getProperty("user.home")
    val xcodeProfilesDir = file("$userHome/Library/Developer/Xcode/UserData/Provisioning Profiles")
    val roboVmProfilesDir = file("$userHome/Library/MobileDevice/Provisioning Profiles")

    from(xcodeProfilesDir) {
        include("*.mobileprovision")
    }
    into(roboVmProfilesDir)
    onlyIf {
        xcodeProfilesDir.isDirectory
    }
}

val iosStageDeviceAppDevicectl = tasks.register<Sync>("iosStageDeviceAppDevicectl") {
    group = iosTaskGroup
    description = "Stage the signed iOS app bundle in a system temporary directory for devicectl"
    dependsOn("iosBuildApp")
    from(iosAppBundle)
    into(iosDevicectlAppBundle)
}

sourceSets {
    main {
        resources.srcDir(generatedDefaultFontAssetsDir)
    }
}

robovm {
    archs = "x86_64:arm64"
    isIosSkipSigning = iosSkipSigning
    iosSignIdentityProperty.orNull?.let {
        iosSignIdentity = it
    }
    iosProvisioningProfileProperty.orNull?.let {
        iosProvisioningProfile = it
    }
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

listOf("launchIOSDevice", "robovmInstall", "robovmArchive", "createIPA").forEach { taskName ->
    tasks.named(taskName).configure {
        dependsOn(iosSyncXcodeProvisioningProfiles)
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

tasks.register<Exec>("iosInstallDeviceDevicectl") {
    group = iosTaskGroup
    description = "Install the signed iOS app bundle on a connected device with Xcode devicectl"
    dependsOn(iosStageDeviceAppDevicectl)
    executable = "xcrun"
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val deviceUdid = iosDeviceUdid.orNull ?: throw org.gradle.api.GradleException(
                "Pass -PiosDeviceUdid=<device udid>. Use :examples:ios:iosListDevices to list connected devices."
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
    description = "Launch the installed iOS app on a connected device with Xcode devicectl"
    dependsOn("iosInstallDeviceDevicectl")
    executable = "xcrun"
    argumentProviders.add(object : org.gradle.process.CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val deviceUdid = iosDeviceUdid.orNull ?: throw org.gradle.api.GradleException(
                "Pass -PiosDeviceUdid=<device udid>. Use :examples:ios:iosListDevices to list connected devices."
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
    description = "Build, install, and launch the websocket example on a connected iOS device with Xcode devicectl"
    dependsOn("iosLaunchDeviceDevicectl")
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

tasks.register<Exec>("iosListDevices") {
    group = iosTaskGroup
    description = "List connected iOS devices and available simulators"
    commandLine("xcrun", "xctrace", "list", "devices")
}
