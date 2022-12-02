import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "com.francisbailey.summitsearch"
version = "1.0.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation("co.elastic.clients:elasticsearch-java:8.5.1")
    implementation("jakarta.json:jakarta.json-api:2.0.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jsoup:jsoup:1.15.3")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.testcontainers", "testcontainers", "1.17.3")
    testImplementation("org.testcontainers", "elasticsearch", "1.17.3")
    testImplementation("jakarta.json.bind", "jakarta.json.bind-api", "2.0.0")

    testImplementation("org.eclipse", "yasson", "2.0.4") {
        exclude(group = "org.glassfish", module = "jakarta.json")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
