import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.owasp.dependencycheck") version "8.2.1"
    kotlin("jvm") version "1.7.21"
}

group = "com.francisbailey.summitsearch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("co.elastic.clients:elasticsearch-java:8.5.1")
    implementation("jakarta.json:jakarta.json-api:2.0.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    implementation("org.testcontainers", "testcontainers", "1.17.3")
    implementation("org.testcontainers", "elasticsearch", "1.17.3")
    implementation("jakarta.json.bind", "jakarta.json.bind-api", "2.0.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
