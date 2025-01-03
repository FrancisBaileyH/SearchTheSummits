import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("org.owasp.dependencycheck") version "8.2.1"
    kotlin("plugin.serialization") version "1.7.21"
    application
}

group = "com.francisbailey.summitsearch"
val ktorVersion = "2.2.2"


repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}