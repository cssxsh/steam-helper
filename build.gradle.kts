plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"

    id("net.mamoe.mirai-console") version "2.12.0-RC"
}

group = "xyz.cssxsh"
version = "1.0.0-dev"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    explicitApi()
}

dependencies {
    implementation("io.ktor:ktor-client-okhttp:2.0.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-encoding:2.0.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-content-negotiation:2.0.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.10.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    api("in.dragonbra:javasteam:1.1.0")
    implementation("net.mamoe:mirai-slf4j-bridge:1.2.0")
    // test
    testImplementation(kotlin("test", "1.6.21"))
    testImplementation("org.slf4j:slf4j-api:1.7.36")
    testImplementation("net.mamoe:mirai-slf4j-bridge:1.2.0")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

