import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

val groupId = "com.github.deedywu.gdx-websockets"
val libraryVersion = "2.0.4-rc2"

extra["GROUPID"] = groupId
extra["VERSION"] = libraryVersion
extra["appName"] = "gdx-websockets"
extra["gdxVersion"] = "1.14.2"
extra["nvVersion"] = "2.14"
extra["roboVMVersion"] = "2.3.24"
extra["teavmVersion"] = "0.15.0"
extra["gdxTeaVMVersion"] = "1.5.6"
extra["gwtVersion"] = "2.11.0"

allprojects {
    val currentProject = this

    apply(plugin = "eclipse")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = uri("https://central.sonatype.com/repository/maven-snapshots/"))
        maven(url = uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        maven(url = uri("https://oss.sonatype.org/content/repositories/releases/"))
        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://teavm.org/maven/repository/")) {
            content {
                includeGroup("org.teavm")
            }
        }
    }

    group = if (currentProject.path.startsWith(":examples:")) "$groupId.examples" else groupId
    version = libraryVersion

    tasks.withType<Test>().configureEach {
        systemProperty("file.encoding", "UTF-8")
    }

    pluginManager.withPlugin("java") {
        val javaRelease = if (currentProject.name.startsWith("teavm")) 11 else 8

        currentProject.extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.toVersion(javaRelease)
            targetCompatibility = JavaVersion.toVersion(javaRelease)
            withSourcesJar()
        }

        currentProject.tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(javaRelease)
        }

        if (!currentProject.path.startsWith(":examples:")) {
            currentProject.extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("mavenJava") {
                        from(currentProject.components["java"])
                        artifactId = if (currentProject.extra.has("ARTIFACTID")) {
                            currentProject.extra["ARTIFACTID"] as String
                        } else {
                            currentProject.name
                        }
                    }
                }
            }
        }
    }
}
