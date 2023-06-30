val ktorVersion = "2.2.2"
val resilience4JVersion = "1.7.0"

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("org.owasp.dependencycheck") version "8.2.1"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.springframework.boot") version "2.7.10"
    application
}

group = "com.francisbailey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://mvn.topobyte.de")
    }
    maven {
        url = uri("https://mvn.slimjars.com")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    implementation("de.topobyte:osm4j-core:0.1.0")
    implementation("de.topobyte:osm4j-xml:0.0.6")
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("co.elastic.clients:elasticsearch-java:8.5.1")
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation(project(":lib:search-index-service"))
    implementation(project(":lib:service-common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("jakarta.json:jakarta.json-api:2.0.1")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}