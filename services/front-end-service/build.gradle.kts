import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.springframework.boot") version "2.7.10"
    id("org.owasp.dependencycheck") version "8.2.1"
    application
}

group = "com.francisbailey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("co.elastic.clients:elasticsearch-java:8.5.1")

    implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:1.7.0")

    implementation(project(":lib:search-index-service"))
    implementation(project(":lib:service-common"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("jakarta.json:jakarta.json-api:2.0.1")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}