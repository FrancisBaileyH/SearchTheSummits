val ktorVersion = "2.2.2"
val resilience4JVersion = "1.7.0"

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.springframework.boot") version "2.7.5"
    application
}

group = "com.francisbailey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(project(":lib:service-common"))

    implementation(project(":api:page-indexing-worker"))
    implementation(project(":lib:indexing-queue-client"))

    implementation(platform("software.amazon.awssdk:bom:2.18.19"))
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.testcontainers", "testcontainers", "1.17.3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}