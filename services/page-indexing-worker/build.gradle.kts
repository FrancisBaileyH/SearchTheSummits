import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

    implementation(platform("software.amazon.awssdk:bom:2.18.19"))
    implementation("software.amazon.awssdk:sqs")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    implementation("io.ktor:ktor-client-core:2.1.3")
    implementation("io.ktor:ktor-client-cio:2.1.3")
    implementation("io.ktor:ktor-client-encoding:2.1.3")

    implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:1.7.0")

    implementation(project(":api:page-indexing-worker"))
    implementation(project(":lib:search-index-service"))
    implementation(project(":lib:service-common"))

    implementation("org.jsoup:jsoup:1.15.3")
    implementation("jakarta.json:jakarta.json-api:2.0.1")

    implementation("redis.clients:jedis:4.3.0")

    testImplementation("io.ktor:ktor-client-mock:2.1.3")
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