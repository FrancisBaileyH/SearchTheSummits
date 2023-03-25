import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.2.2"
val resilience4JVersion = "1.7.0"

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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(platform("software.amazon.awssdk:bom:2.20.31"))
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:s3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    implementation("com.sksamuel.scrimage:scrimage-core:4.0.33")
    implementation("org.apache.pdfbox:pdfbox:2.0.27")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")

    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4JVersion")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:$resilience4JVersion")

    implementation("co.elastic.clients:elasticsearch-java:8.5.1")

    implementation(project(":api:page-indexing-worker"))
    implementation(project(":lib:search-index-service"))
    implementation(project(":lib:service-common"))
    implementation(project(":lib:kotlin-htmldate"))
    implementation(project(":lib:indexing-queue-client"))
    implementation("org.imgscalr:imgscalr-lib:4.2")

    implementation("org.jsoup:jsoup:1.15.3")
    implementation("jakarta.json:jakarta.json-api:2.0.1")

    implementation("redis.clients:jedis:4.3.0")

    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
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