plugins {
    application
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("net.dv8tion:JDA:6.6.0") {
      exclude(module="opus-java")
      exclude(module="tink")
    }

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    implementation(libs.hikaricp)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "me.eeshe.celeoproc.CeleoprocApp"
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

// Load key=value pairs from the root .env file for local development convenience.
// Real environment variables take precedence over the file.
fun loadEnvFile(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val cleaned = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
            val separator = cleaned.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = cleaned.substring(0, separator)
            val value = cleaned.substring(separator + 1).trim().trim('"', '\'')
            key to value
        }
        .toMap()
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    environment(loadEnvFile(rootDir.resolve(".env")).filterKeys { !System.getenv().containsKey(it) })
}
