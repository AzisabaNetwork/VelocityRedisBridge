plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "net.azisaba"
version = "2.1.0"
description = "Plugin to help parallelization of Velocity."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.3.1")
    annotationProcessor("com.velocitypowered:velocity-api:4.3.1")
    implementation("redis.clients:jedis:8.0.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 17
    }

    jar {
        archiveClassifier = "original"
    }

    shadowJar {
        archiveFileName = "${project.name}.jar"
    }

    build {
        dependsOn(shadowJar)
    }
}
