plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.6.7"
}

group = "io.github.gnuf0rce"
version = "0.1.0-dev-1"

repositories {
    mavenLocal()
    maven(url = "https://maven.aliyun.com/repository/public")
    mavenCentral()
    jcenter()
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
            languageSettings.useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
        }
        test {
            //
        }
    }
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
    configureShadow {
        exclude {
            it.path.startsWith("kotlin")
        }
//        exclude {
//            val features = listOf("features")
//            it.path.startsWith("io/ktor") && it.path.startsWith("io/ktor/client/features/compression").not()
//        }
//        exclude {
//            it.path.startsWith("okhttp3/internal")
//        }
//        exclude {
//            it.path.startsWith("okio")
//        }
    }
}

dependencies {
    implementation(ktor("client-encoding", Versions.ktor)) {
        exclude(group = "io.ktor", module = "client-core")
    }
    implementation(ktor("client-serialization", Versions.ktor)) {
        exclude(group = "io.ktor", module = "client-core")
    }
    // compileOnly(okhttp3("okhttp", Versions.okhttp))
    implementation(okhttp3("okhttp-dnsoverhttps", Versions.okhttp)) {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation(jsoup(Versions.jsoup))
    // test
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = Versions.junit)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

