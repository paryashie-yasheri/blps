import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.apply

class TestContainerManager(private val imageName: String = "postgres:16.6-alpine") {
    private var container: PostgreSQLContainer<*>? = null

    fun start(): PostgreSQLContainer<*> {
        if (container == null) {
            println("Starting Testcontainers with image: $imageName")
            try {
                container =
                    PostgreSQLContainer<Nothing>(DockerImageName.parse(imageName).asCompatibleSubstituteFor("postgres"))
                        .apply {
                            withDatabaseName("study")
                            withUsername("test")
                            withPassword("test")
                            withExposedPorts(5432)
                            start()
                        }
                println("Container started: ${container?.containerId?.substring(0, 12)}")
            } catch (e: Exception) {
                println("Failed to start container: ${e.message}")
                throw e
            }
        }
        return container!!
    }

    fun toPostgresEnv(schema: String = "public"): Map<String, String> {
        val c = container ?: throw IllegalStateException("Container not started")
        return mapOf(
            "POSTGRES_HOST" to c.host,
            "POSTGRES_PORT" to c.firstMappedPort.toString(),
            "POSTGRES_DB" to c.databaseName,
            "POSTGRES_USER" to c.username,
            "POSTGRES_PASSWORD" to c.password,
            "POSTGRES_SCHEMA" to schema
        )
    }

    fun stop() {
        println("Stopping container...")
        container?.stop()
        container = null
    }

    val jdbcUrl: String get() = container?.jdbcUrl ?: throw IllegalStateException("Container not started")
    val username: String get() = container?.username ?: "test"
    val password: String get() = container?.password ?: "test"
    val port: String get() = "5432"
}

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("com.google.cloud.tools.jib") version "3.5.2"
}

group = "soaringLizards"
version = "0.0.1-SNAPSHOT"
description = "blps"

jib {
    to {
        image = "blps/backend:jib-dev"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
    sourceSets {
        val main by getting {
            kotlin.srcDir("build/generated/main/kotlin")
        }
    }
}

val testcontainersVersion = "1.21.4"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.5.0")
        classpath("org.testcontainers:postgresql:1.21.4")
        classpath("org.jooq:jooq-codegen:3.18.7")
    }
}

repositories {
    mavenCentral()
}

configurations {
    create("jooqGenerator")
    create("liquibaseRuntime")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-webservices")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    add("jooqGenerator", "org.jooq:jooq-codegen")
    add("jooqGenerator", "org.jooq:jooq-meta")
    add("jooqGenerator", "org.postgresql:postgresql")
    add("liquibaseRuntime", "org.liquibase:liquibase-core")
    add("liquibaseRuntime", "org.postgresql:postgresql")
    add("liquibaseRuntime", "info.picocli:picocli:4.7.6")
    add("liquibaseRuntime", "com.h2database:h2:2.2.224")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webservices-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:testcontainers") {
        version { strictly(testcontainersVersion) }
    }
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy {
        force("org.testcontainers:testcontainers:$testcontainersVersion")
        force("org.testcontainers:jdbc:$testcontainersVersion")
        force("org.testcontainers:database-commons:$testcontainersVersion")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.test {
    environment("api.version", "1.44")
    systemProperty("api.version", "1.44")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

val dotenv: Map<String, String> = run {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) emptyMap() else envFile.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) null
            else trimmed.substringBefore("=").trim() to trimmed.substringAfter("=").trim().trim('"', '\'')
        }
        .toMap()
}

fun createEnvMap(): Map<String, String> {
    return dotenv.ifEmpty { System.getenv() };
}

fun createLiquibaseArgs(env: Map<String, String>): List<String> {
    val url = "jdbc:postgresql://${env["POSTGRES_HOST"]}:${env["POSTGRES_PORT"]}/${env["POSTGRES_DB"]}"

    return mutableListOf(
        "--changeLogFile=src/main/resources/db/db.changelog-master.yaml",
        "--url=$url",
        "--username=${env["POSTGRES_USER"]}",
        "--password=${env["POSTGRES_PASSWORD"]}",
        "--defaultSchemaName=${env["POSTGRES_SCHEMA"]}",
        "--liquibaseSchemaName=${env["POSTGRES_SCHEMA"]}",
    )
}

val liquibaseUpdate by tasks.register<JavaExec>("liquibaseUpdate") {
    group = "liquibase"
    description = "Apply database migrations before code generation"

    classpath = configurations["liquibaseRuntime"]
    mainClass.set("liquibase.integration.commandline.LiquibaseCommandLine")

    val connectionArgs = createLiquibaseArgs(createEnvMap()).toMutableList()
    connectionArgs += "update"

    args = connectionArgs
}

var containerInstance: TestContainerManager? = null

fun getOrCreateContainer(): TestContainerManager {
    if (containerInstance == null) {
        containerInstance = TestContainerManager()
    }
    containerInstance!!.start()
    return containerInstance!!
}

val liquibaseUpdateTestContainer by tasks.register<JavaExec>("liquibaseUpdateTestContainer") {
    group = "liquibase"
    description = "Apply database migrations before code generation"
    classpath = configurations["liquibaseRuntime"]
    mainClass.set("liquibase.integration.commandline.Main")

    doFirst {
        val manager = getOrCreateContainer()

        val connectionArgs = createLiquibaseArgs(manager.toPostgresEnv()).toMutableList()
        connectionArgs += "update"

        args = connectionArgs
    }
}
tasks.register<JavaExec>("generateJooq") {
    group = "jooq"
    classpath = configurations["jooqGenerator"]
    mainClass.set("org.jooq.codegen.GenerationTool")
    args = listOf("src/main/resources/jooq-config.xml")
    outputs.upToDateWhen { false }

    doFirst {
        val manager = getOrCreateContainer()
        val pgConfig = manager.toPostgresEnv()

        environment(pgConfig)
        pgConfig.forEach { (k, v) -> systemProperty(k, v) }
    }

    doLast {
        containerInstance?.stop()
    }
}

tasks.register<JavaExec>("generateJooqFromExistingDb") {
    group = "jooq"
    classpath = configurations["jooqGenerator"]
    mainClass.set("org.jooq.codegen.GenerationTool")
    args = listOf("src/main/resources/jooq-config.xml")
    outputs.upToDateWhen { false }

    doFirst {
        val genEnv = createEnvMap()
        environment(createEnvMap())
        genEnv.forEach { (k, v) -> systemProperty(k, v) }
    }

    doLast {
        containerInstance?.stop()
    }
}

tasks.named("generateJooq").configure {
    dependsOn(liquibaseUpdateTestContainer)
}

tasks.named("generateJooqFromExistingDb").configure {
    dependsOn(liquibaseUpdate)
}

tasks.matching { it.name == "generateSources" }.configureEach {
    dependsOn(liquibaseUpdate)
}


tasks.register("checkDocker") {
    doLast {
        println("Docker available: ${org.testcontainers.DockerClientFactory.instance().isDockerAvailable}")
    }
}