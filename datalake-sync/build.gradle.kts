plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.soaringLizards"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

graalvmNative {
    toolchainDetection = true
    binaries {
        named("main") {
            buildArgs.addAll(
                "--initialize-at-build-time=org.slf4j,ch.qos.logback",
                "--initialize-at-build-time=org.apache.commons,org.apache.commons.text",
                "--initialize-at-build-time=com.fasterxml.jackson,org.yaml.snakeyaml",
                "--initialize-at-build-time=sun.security.ssl",
                "--initialize-at-build-time=org.springframework,org.quartz",
                "--initialize-at-build-time=soaringlizards",
                "--initialize-at-build-time=org.postgresql",
                "-H:+AddAllCharsets",
                "-H:+ReportExceptionStackTraces",
                "-H:EnableURLProtocols=http,https",
            )
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}
