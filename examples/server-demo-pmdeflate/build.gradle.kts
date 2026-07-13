plugins {
    application
}

val nettyVersion = "4.2.15.Final"

application {
    mainClass.set("com.github.czyzby.websocket.examples.pmdeflateserver.PerMessageDeflateNettyServer")
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
}
