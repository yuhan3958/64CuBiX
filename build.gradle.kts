plugins {
    java
    application
}

group = "me.cubix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.4"

fun lwjglNatives(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "natives-windows"
        os.contains("mac") -> "natives-macos"
        else -> "natives-linux"
    }
}

val natives = lwjglNatives()

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-nuklear")

    runtimeOnly("org.lwjgl:lwjgl::$natives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$natives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$natives")
    runtimeOnly("org.lwjgl:lwjgl-nuklear::$natives")

    implementation("org.joml:joml:1.10.6")
}

application {
    mainClass.set("me.cubix.Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Dorg.lwjgl.util.Debug=true",
        "-Dorg.lwjgl.util.DebugLoader=true"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}