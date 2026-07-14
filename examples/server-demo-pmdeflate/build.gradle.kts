plugins {
    application
}

val nettyVersion = "4.2.15.Final"
val bouncyCastleVersion = "1.70"

application {
    mainClass.set("com.github.czyzby.websocket.examples.pmdeflateserver.PerMessageDeflateNettyServer")
}

tasks.withType<JavaExec>().configureEach {
    workingDir = project.projectDir
}

dependencies {
    implementation("io.netty:netty-common:$nettyVersion")
    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-transport:$nettyVersion")
    implementation("io.netty:netty-resolver:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-codec-base:$nettyVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
    implementation("io.netty:netty-codec-compression:$nettyVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcutil-jdk15on:$bouncyCastleVersion")
}
